package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka

import arrow.core.Either
import arrow.core.NonEmptyList
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.ApplicationInfo
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.clients.pdl.PdlHentForenkletStatus
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.clients.pdl.PdlHentPerson
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.serdes.HendelseState
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.metrics.tellPdlAvsluttetHendelser
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.metrics.tellStatusFraPdlHentPersonBolk
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils.genererPersonFakta
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils.negativeOpplysninger
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils.statusToOpplysningMap
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils.toAarsak
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils.toPerson
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.pdl.graphql.generated.hentforenkletstatusbolk.Folkeregisterpersonstatus
import no.nav.paw.pdl.graphql.generated.hentforenkletstatusbolk.HentPersonBolkResult as ForenkletStatusBolkResult
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.HentPersonBolkResult
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.Person
import org.apache.kafka.streams.processor.Cancellable
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.KeyValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*

data class EvalueringResultat(
    val grunnlagV1: List<Folkeregisterpersonstatus>? = null,
    val grunnlagV2: Either<NonEmptyList<Problem>, GrunnlagForGodkjenning>? = null,
    val hendelseState: HendelseState,
    val avsluttPeriode: Boolean,
    val slettForhaandsGodkjenning: Boolean
)

fun scheduleAvsluttPerioder(
    ctx: ProcessorContext<Long, Hendelse>,
    hendelseStateStore: KeyValueStore<UUID, HendelseState>,
    interval: Duration = Duration.ofDays(1),
    pdlHentForenkletStatus: PdlHentForenkletStatus,
    pdlHentPersonBolk: PdlHentPerson,
    prometheusMeterRegistry: PrometheusMeterRegistry,
): Cancellable = ctx.schedule(interval, PunctuationType.WALL_CLOCK_TIME) {

    val logger = LoggerFactory.getLogger("scheduleAvsluttPerioder")

    hendelseStateStore.all().use { iterator ->
        iterator
            .asSequence()
            .toList()
            .filterValidHendelseStates()
            .chunked(1000) { chunk ->
                val identitetsnummere = chunk.map { it.value.identitetsnummer }

                // Versjon 2
                val pdlHentPersonResults = hentPersonBolk(identitetsnummere, pdlHentPersonBolk)
                val resultaterV2: List<EvalueringResultat> = if (pdlHentPersonResults == null) {
                    logger.warn("PDL hentPersonBolk returnerte null")
                    emptyList()
                } else {
                    pdlHentPersonResults.processPdlResultsV2(
                        chunk,
                        logger
                    )
                }

                // Versjon 1
                val pdlHentForenkletStatusResults = hentForenkletStatus(identitetsnummere, pdlHentForenkletStatus)
                val resultaterV1: List<EvalueringResultat> = if (pdlHentForenkletStatusResults == null) {
                    logger.warn("PDL hentForenkletStatus returnerte null")
                    emptyList()
                } else {
                    pdlHentForenkletStatusResults.processResults(
                        chunk,
                        prometheusMeterRegistry,
                        logger
                    )
                }

                resultaterV1.compareResults(resultaterV2, logger)
                resultaterV1.onEach { resultat ->
                    val folkeregisterpersonstatus = resultat.grunnlagV1
                    val hendelseState = resultat.hendelseState
                    if (resultat.avsluttPeriode && folkeregisterpersonstatus != null) {
                        sendAvsluttetHendelse(folkeregisterpersonstatus, hendelseState, hendelseStateStore, ctx, prometheusMeterRegistry)
                    }
                }.forEach { resultat ->
                    val hendelseState = resultat.hendelseState
                    if (resultat.slettForhaandsGodkjenning) {
                        slettForhaandsGodkjenning(hendelseState, hendelseStateStore)
                    }
                }
            }
    }
}

fun List<ForenkletStatusBolkResult>.processResults(
    chunk: List<KeyValue<UUID, HendelseState>>,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    logger: Logger
): List<EvalueringResultat> =
    this.filter { result ->
        prometheusMeterRegistry.tellStatusFraPdlHentPersonBolk(result.code)
        if (result.code in pdlErrorResponses) {
            logger.error("Feil ved henting av forenklet status fra PDL: ${result.code}")
            false
        } else true
    }.mapNotNull { result ->
        hentFolkeregisterpersonstatusOgHendelseState(result, chunk)
    }.map { (folkeregisterpersonstatus, hendelseState) ->

        val avsluttPeriode = !folkeregisterpersonstatus.erBosattEtterFolkeregisterloven &&
                folkeregisterpersonstatus.filterAvsluttPeriodeGrunnlag(hendelseState.opplysninger).isNotEmpty()
        val skalSletteForhaandsGodkjenning = skalSletteForhaandsGodkjenning(hendelseState, avsluttPeriode)

        EvalueringResultat(
            grunnlagV1 = folkeregisterpersonstatus,
            hendelseState = hendelseState,
            avsluttPeriode = avsluttPeriode,
            slettForhaandsGodkjenning = skalSletteForhaandsGodkjenning
        )
    }

fun isPdlResultOK(code: String, logger: Logger): Boolean =
    if(code in pdlErrorResponses){
        logger.error("Feil ved henting av Person fra PDL: $code")
        false
    } else true

fun getHendelseStateAndPerson(
    result: HentPersonBolkResult,
    chunk: List<KeyValue<UUID, HendelseState>>,
    logger: Logger
): Pair<Person, HendelseState>? {
    val person = result.person
    if(person == null){
        logger.error("Person er null for periodeId: ${chunk.find { it.value.identitetsnummer == result.ident }?.key}")
        return null
    }
    val hendelseState = chunk.find { it.value.identitetsnummer == result.ident }
        ?.value ?: return null

    return Pair(person, hendelseState)
}

fun Set<Opplysning>.toDomeneOpplysninger() = this
    .filterNot { it == Opplysning.FORHAANDSGODKJENT_AV_ANSATT }
    .mapNotNull { hendelseOpplysningTilDomeneOpplysninger(it) }
    .toSet()

fun Set<Opplysning>.erForhaandsGodkjent() = Opplysning.FORHAANDSGODKJENT_AV_ANSATT in this

fun skalAvsluttePeriode(
    pdlEvaluering: Either<NonEmptyList<Problem>, GrunnlagForGodkjenning>,
    opplysningerEvaluering: Either<NonEmptyList<Problem>, GrunnlagForGodkjenning>,
    erForhaandsgodkjent: Boolean
) = pdlEvaluering.fold(
    { pdlEvalueringLeft ->
        when(opplysningerEvaluering) {
            is Either.Left -> {
                !(erForhaandsgodkjent && opplysningerEvaluering.value.map { it.regel.id }.containsAll(pdlEvalueringLeft.map { it.regel.id }))
            }
            is Either.Right -> {
                true
            }
        }
    },
    { false }
)

fun List<HentPersonBolkResult>.processPdlResultsV2(
    chunk: List<KeyValue<UUID, HendelseState>>,
    logger: Logger
): List<EvalueringResultat> =
    this.filter { result -> isPdlResultOK(result.code, logger) }
    .mapNotNull { result -> getHendelseStateAndPerson(result, chunk, logger) }
    .map { (person, hendelseState) ->
        val hendelseOpplysninger = hendelseState.opplysninger

        val domeneOpplysninger = hendelseOpplysninger.toDomeneOpplysninger()

        val opplysningerEvaluering = InngangsRegler.evaluer(domeneOpplysninger)
        val pdlEvaluering = InngangsRegler.evaluer(genererPersonFakta(person.toPerson()))

        val erForhaandsgodkjent = hendelseOpplysninger.erForhaandsGodkjent()

        val skalAvsluttePeriode = skalAvsluttePeriode(pdlEvaluering, opplysningerEvaluering, erForhaandsgodkjent)

        val slettForhaandsGodkjenning = pdlEvaluering.isRight() && opplysningerEvaluering.isLeft() && erForhaandsgodkjent

        EvalueringResultat(
            grunnlagV2 = pdlEvaluering,
            hendelseState = hendelseState,
            avsluttPeriode = skalAvsluttePeriode,
            slettForhaandsGodkjenning = slettForhaandsGodkjenning
        )
    }

fun Either<NonEmptyList<Problem>, GrunnlagForGodkjenning>.toAarsak(): String =
    this.fold(
        { problems ->
            problems.joinToString(", ") { problem -> problem.regel.id.beskrivelse }
        },
        { "Ingen årsak" }
    )

fun skalSletteForhaandsGodkjenning(
    hendelseState: HendelseState,
    avsluttPeriode: Boolean
) = Opplysning.FORHAANDSGODKJENT_AV_ANSATT in hendelseState.opplysninger
            && hendelseState.opplysninger.any { it in negativeOpplysninger } && !avsluttPeriode

fun sendAvsluttetHendelse(
    folkeregisterpersonstatus: List<Folkeregisterpersonstatus>,
    hendelseState: HendelseState,
    hendelseStateStore: KeyValueStore<UUID, HendelseState>,
    ctx: ProcessorContext<Long, Hendelse>,
    prometheusMeterRegistry: PrometheusMeterRegistry,
) {
    val aarsak = folkeregisterpersonstatus
        .filterAvsluttPeriodeGrunnlag(hendelseState.opplysninger)
        .ifEmpty {
            return
        }
        .toAarsak()

    val avsluttetHendelse = genererAvsluttetHendelseRecord(hendelseState, aarsak)
    ctx.forward(avsluttetHendelse)
        .also {
            prometheusMeterRegistry.tellPdlAvsluttetHendelser(aarsak)
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

fun List<EvalueringResultat>.compareResults(
    other: List<EvalueringResultat>,
    logger: Logger
) {
    val evalueringResultaterV1 = this.associateBy { it.hendelseState.periodeId }
    val evalueringResultaterV2 = other.associateBy { it.hendelseState.periodeId }

    val matchingPeriodeIder = evalueringResultaterV1.keys.intersect(evalueringResultaterV2.keys)

    matchingPeriodeIder.forEach { periodeId ->
        val resultatV1 = evalueringResultaterV1[periodeId]
        val resultatV2 = evalueringResultaterV2[periodeId]

        if (resultatV1 == null || resultatV2 == null) {
            logger.error("Missing result for periodeId: $periodeId in either v1 or v2")
            return@forEach
        }

        if (resultatV1.avsluttPeriode != resultatV2.avsluttPeriode) {
            logger.warn(
                "AvsluttPeriode mismatch for periodeId: $periodeId, " +
                        "v1: ${resultatV1.avsluttPeriode}, aarsak: ${resultatV1.grunnlagV1?.filterAvsluttPeriodeGrunnlag(resultatV1.hendelseState.opplysninger)?.toAarsak()}, " +
                        "v2: ${resultatV2.avsluttPeriode}, aarsak: ${resultatV2.grunnlagV2?.toAarsak()}"
            )
        }

        if (resultatV1.slettForhaandsGodkjenning != resultatV2.slettForhaandsGodkjenning) {
            logger.warn(
                "SlettForhaandsGodkjenning mismatch for periodeId: $periodeId, " +
                        "v1: ${resultatV1.slettForhaandsGodkjenning}, " +
                        "v2: ${resultatV2.slettForhaandsGodkjenning}"
            )
        }
    }
}

fun hentFolkeregisterpersonstatusOgHendelseState(
    result: ForenkletStatusBolkResult,
    chunk: List<KeyValue<UUID, HendelseState>>
): Pair<List<Folkeregisterpersonstatus>, HendelseState>? {
    val person = result.person ?: return null
    val hendelseState = chunk.find { it.value.identitetsnummer == result.ident }
        ?.value ?: return null

    return Pair(person.folkeregisterpersonstatus, hendelseState)
}

fun hentForenkletStatus(
    identitetsnummere: List<String>,
    pdlHentForenkletStatus: PdlHentForenkletStatus,
): List<ForenkletStatusBolkResult>? {
    return pdlHentForenkletStatus.hentForenkletStatus(
        identitetsnummere,
        UUID.randomUUID().toString(),
        "paw-arbeidssoekerregisteret-utgang-pdl",
    )
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

fun List<Folkeregisterpersonstatus>.filterAvsluttPeriodeGrunnlag(
    opplysningerFraStartetHendelse: Set<Opplysning>
): Set<Opplysning> {
    val isForhaandsGodkjent = Opplysning.FORHAANDSGODKJENT_AV_ANSATT in opplysningerFraStartetHendelse

    return this.asSequence()
        .mapNotNull { status -> statusToOpplysningMap[status.forenkletStatus] }
        .filter { it in negativeOpplysninger }
        // Opplysning FORHAANDSGODKJENT_AV_ANSATT + DOED/SAVNET/IKKE_BOSATT/OPPHOERT_IDENTITET skal overstyre tilsvarende forenkletStatus fra PDL
        .filterNot { it in opplysningerFraStartetHendelse && isForhaandsGodkjent }
        .toSet()
}


val List<Folkeregisterpersonstatus>.erBosattEtterFolkeregisterloven
    get(): Boolean =
        this.any { it.forenkletStatus == "bosattEtterFolkeregisterloven" }

val pdlErrorResponses = setOf(
    "bad_request",
    "not_found"
)

fun genererAvsluttetHendelseRecord(
    hendelseState: HendelseState,
    aarsak: String,
): Record<Long, Avsluttet> = Record(
    hendelseState.recordKey,
    Avsluttet(
        hendelseId = hendelseState.periodeId,
        id = hendelseState.brukerId ?: throw IllegalStateException("BrukerId er null"),
        identitetsnummer = hendelseState.identitetsnummer,
        metadata = Metadata(
            tidspunkt = Instant.now(),
            aarsak = aarsak,
            kilde = "paw-arbeidssoekerregisteret-utgang-pdl",
            utfoertAv = Bruker(
                type = BrukerType.SYSTEM,
                id = ApplicationInfo.id
            )
        )
    ),
    Instant.now().toEpochMilli()
)
