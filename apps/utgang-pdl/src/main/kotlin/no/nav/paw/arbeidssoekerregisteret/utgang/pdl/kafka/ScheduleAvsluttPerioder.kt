package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.ApplicationInfo
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.clients.pdl.PdlHentPerson
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.serdes.Endring
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.serdes.HendelseState
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.serdes.OK
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.serdes.UDENFINERT
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.metrics.tellEndring
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.metrics.tellPdlAvsluttetHendelser
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils.*
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.HentPersonBolkResult
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Person
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.processor.Cancellable
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

data class EvalueringResultat(
    val hendelseState: HendelseState,
    val grunnlag: Set<RegelId>,
    val detaljer: Set<no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning>,
    val avsluttPeriode: Boolean,
    val slettForhaandsGodkjenning: Boolean,
    val endring: Endring?
)

fun scheduleAvsluttPerioder(
    ctx: ProcessorContext<Long, Hendelse>,
    hendelseStateStore: KeyValueStore<UUID, HendelseState>,
    interval: Duration,
    schduledInterval: Duration,
    pdlHentPersonBolk: PdlHentPerson,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    regler: Regler,
    sisteKjoeringStateStore: KeyValueStore<Int, Long>
): Cancellable = ctx.schedule(schduledInterval, PunctuationType.WALL_CLOCK_TIME) {

    val logger = LoggerFactory.getLogger("scheduleAvsluttPerioder")
    val partition = ctx.taskId().partition()
    val sisteKjoering = sisteKjoeringStateStore?.get(partition) ?: 0L
    val tidSidenSist = Duration.ofMillis(ctx.currentSystemTimeMs() - sisteKjoering)
    if (tidSidenSist >= interval) {
        logger.info("Kjører 'scheduleAvsluttPerioder' for partisjon $partition, tid siden sist: $tidSidenSist (tid mellom hver kjøring: $interval)")
        hendelseStateStore.all().use { iterator ->
            iterator
                .asSequence()
                .toList()
                .filterValidHendelseStates()
                .chunked(1000) { chunk ->
                    val identitetsnummere = chunk.map { it.value.identitetsnummer }

                    val pdlHentPersonResults = hentPersonBolk(identitetsnummere, pdlHentPersonBolk)
                    val resultater: List<EvalueringResultat> = if (pdlHentPersonResults == null) {
                        logger.warn("PDL hentPersonBolk returnerte null")
                        emptyList()
                    } else {
                        pdlHentPersonResults.processPdlResultsV2(
                            prometheusMeterRegistry = prometheusMeterRegistry,
                            regler = regler,
                            chunk = chunk,
                            logger = logger
                        )
                    }

                    resultater.forEach { resultat ->
                        val hendelseState = resultat.hendelseState
                        if (resultat.endring != null) {
                            val nyState = hendelseState.copy(sisteEndring = resultat.endring)
                            hendelseStateStore.put(nyState.periodeId, nyState)
                            prometheusMeterRegistry.tellEndring(
                                tidspunktForrigeEndring = hendelseState.sisteEndring?.tidspunkt
                                    ?: hendelseState.startetTidspunkt,
                                endring = resultat.endring,
                            )
                        }
                        if (resultat.avsluttPeriode) {
                            sendAvsluttetHendelse(
                                resultat.grunnlag,
                                resultat.detaljer,
                                hendelseState,
                                hendelseStateStore,
                                ctx,
                                prometheusMeterRegistry
                            )
                        }
                        if (resultat.slettForhaandsGodkjenning) {
                            slettForhaandsGodkjenning(hendelseState, hendelseStateStore)
                        }
                    }
                }
        }
        sisteKjoeringStateStore.put(partition, ctx.currentSystemTimeMs())
    } else {
        logger.info("Dropper kjøring av 'scheduleAvsluttPerioder' for partisjon $partition, tid siden sist: $tidSidenSist, tid mellom hver kjøring: $interval")
    }
}

fun isPdlResultOK(code: String, logger: Logger): Boolean =
    if (code in pdlErrorResponses) {
        logger.error("Feil ved henting av Person fra PDL: $code")
        false
    } else true

fun getHendelseStateAndPerson(
    result: HentPersonBolkResult,
    chunk: List<KeyValue<UUID, HendelseState>>,
    logger: Logger
): Pair<Person, HendelseState>? {
    val person = result.person
    if (person == null) {
        logger.error("Person er null for periodeId: ${chunk.find { it.value.identitetsnummer == result.ident }?.key}")
        return null
    }
    val hendelseState = chunk.find { it.value.identitetsnummer == result.ident }
        ?.value ?: return null

    return Pair(person, hendelseState)
}

fun Set<Opplysning>.toDomeneOpplysninger() = this
    .mapNotNull { hendelseOpplysningTilDomeneOpplysninger(it) }
    .toSet()

fun List<HentPersonBolkResult>.processPdlResultsV2(
    prometheusMeterRegistry: PrometheusMeterRegistry,
    regler: Regler,
    chunk: List<KeyValue<UUID, HendelseState>>,
    logger: Logger
): List<EvalueringResultat> =
    this.filter { result -> isPdlResultOK(result.code, logger) }
        .mapNotNull { result -> getHendelseStateAndPerson(result, chunk, logger) }
        .onEach { (person, _) ->
            runCatching {
                val oppholdsInfo = person.opphold.firstOrNull()?.let { opphold ->
                    statsOppholdstilatelse(
                        fra = opphold.oppholdFra,
                        til = opphold.oppholdTil,
                        type = opphold.type.name
                    )
                }
                val personFakta = genererPersonFakta(person.toPerson())
                prometheusMeterRegistry.oppholdstillatelseStats(oppholdsInfo, personFakta)
            }.onFailure { ex ->
                logger.warn("Feil under stats generering", ex)
            }
        }
        .map { (person, hendelseState) ->
            val registreringsOpplysninger = hendelseState.opplysninger.toDomeneOpplysninger()
            val gjeldeneOpplysninger = genererPersonFakta(person.toPerson())

            val resultat = prosesser(
                regler = regler,
                inngangsOpplysninger = registreringsOpplysninger,
                gjeldeneOpplysninger = gjeldeneOpplysninger
            )
            val forrigeRegelId = hendelseState.sisteEndring?.tilRegelId ?: UDENFINERT
            val nyRegelId = if (resultat.periodeSkalAvsluttes) {
                resultat.grunnlag.first::class.simpleName ?: "ukjent"
            } else {
                OK
            }
            val endring = if (forrigeRegelId != nyRegelId) {
                Endring(
                    fraRegelId = forrigeRegelId,
                    tilRegelId = nyRegelId,
                    tidspunkt = Instant.now()
                )
            } else {
                null
            }

            EvalueringResultat(
                hendelseState = hendelseState,
                grunnlag = resultat.grunnlag.toSet(),
                detaljer = gjeldeneOpplysninger.toSet(),
                avsluttPeriode = resultat.periodeSkalAvsluttes,
                slettForhaandsGodkjenning = resultat.forhaandsgodkjenningSkalSlettes,
                endring = endring
            )
        }


fun sendAvsluttetHendelse(
    grunnlag: Set<RegelId>,
    detaljer: Set<no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning>,
    hendelseState: HendelseState,
    hendelseStateStore: KeyValueStore<UUID, HendelseState>,
    ctx: ProcessorContext<Long, Hendelse>,
    prometheusMeterRegistry: PrometheusMeterRegistry,
) {
    val avsluttetHendelse = genererAvsluttetHendelseRecord(hendelseState, grunnlag, detaljer)
    ctx.forward(avsluttetHendelse)
        .also {
            prometheusMeterRegistry.tellPdlAvsluttetHendelser(grunnlag.joinToString { it.beskrivelse })
            hendelseStateStore.delete(hendelseState.periodeId)
        }
}

fun slettForhaandsGodkjenning(hendelseState: HendelseState, hendelseStateStore: KeyValueStore<UUID, HendelseState>) {
    val oppdatertHendelseState = hendelseState.copy(
        opplysninger = hendelseState.opplysninger
            .filterNot { it == Opplysning.FORHAANDSGODKJENT_AV_ANSATT || it in negativeOpplysninger }
            .toSet()
    )
    hendelseStateStore.put(hendelseState.periodeId, oppdatertHendelseState)
}

fun hentPersonBolk(
    identitetsnummere: List<String>,
    pdlHentPersonBolk: PdlHentPerson,
): List<HentPersonBolkResult>? {
    return pdlHentPersonBolk.hentPerson(
        identitetsnummere,
        UUID.randomUUID().toString(),
        "paw-arbeidssoekerregisteret-utgang-pdl",
    )
}

fun List<KeyValue<UUID, HendelseState>>.filterValidHendelseStates(): List<KeyValue<UUID, HendelseState>> =
    this.filter { entry ->
        val hendelseState = entry.value
        hendelseState.harTilhoerendePeriode && hendelseState.brukerId != null
    }

val pdlErrorResponses = setOf(
    "bad_request",
    "not_found"
)

fun genererAvsluttetHendelseRecord(
    hendelseState: HendelseState,
    grunnlag: Set<RegelId>,
    detaljer: Set<no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning>
): Record<Long, Avsluttet> = Record(
    hendelseState.recordKey,
    Avsluttet(
        hendelseId = hendelseState.periodeId,
        id = hendelseState.brukerId ?: throw IllegalStateException("BrukerId er null"),
        identitetsnummer = hendelseState.identitetsnummer,
        metadata = Metadata(
            tidspunkt = Instant.now(),
            aarsak = grunnlag.joinToString { it.beskrivelse },
            kilde = "paw-arbeidssoekerregisteret-utgang-pdl",
            utfoertAv = Bruker(
                type = BrukerType.SYSTEM,
                id = ApplicationInfo.id,
                sikkerhetsnivaa = null
            )
        ),
        opplysninger = detaljer.filterIsInstance<DomeneOpplysning>().map(::domeneOpplysningTilHendelseOpplysning)
            .toSet()
    ),
    Instant.now().toEpochMilli()
)
