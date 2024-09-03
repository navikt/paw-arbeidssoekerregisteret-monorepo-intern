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
    val grunnlag: Pair<List<Folkeregisterpersonstatus>?, HendelseState>,
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
                    val (folkeregisterpersonstatus, hendelseState) = resultat.grunnlag
                    if (resultat.avsluttPeriode && folkeregisterpersonstatus != null) {
                        sendAvsluttetHendelse(folkeregisterpersonstatus, hendelseState, hendelseStateStore, ctx, prometheusMeterRegistry)
                    }
                }.forEach { resultat ->
                    val hendelseState = resultat.grunnlag.second
                    if (resultat.slettForhaandsGodkjenning) {
                        slettForhaandsGodkjenning(hendelseState, hendelseStateStore)
                    }
                }
            }
    }
}

private fun List<ForenkletStatusBolkResult>.processResults(
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

        val avsluttPeriode = !folkeregisterpersonstatus.erBosattEtterFolkeregisterloven
        val skalSletteForhaandsGodkjenning = skalSletteForhaandsGodkjenning(hendelseState, avsluttPeriode)

        EvalueringResultat(
            Pair(folkeregisterpersonstatus, hendelseState),
            avsluttPeriode,
            skalSletteForhaandsGodkjenning
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
    .map { hendelseOpplysningTilDomeneOpplysninger(it) as no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning }
    .toSet()

fun Set<Opplysning>.erForhaandsGodkkjent() = Opplysning.FORHAANDSGODKJENT_AV_ANSATT in this

fun skalAvsluttePeriode(
    pdlEvaluering: Either<NonEmptyList<Problem>, GrunnlagForGodkjenning>,
    opplysningerEvaluering: Either<NonEmptyList<Problem>, GrunnlagForGodkjenning>,
    erForhaandsgodkjent: Boolean
) = pdlEvaluering.fold(
    { pdlEvalueringLeft ->
        when(opplysningerEvaluering) {
            is Either.Left -> {
                !(erForhaandsgodkjent && opplysningerEvaluering.value.containsAnyOf(pdlEvalueringLeft))
            }
            is Either.Right -> {
                true
            }
        }
    },
    { false }
)

fun NonEmptyList<Problem>.containsAnyOf(other: NonEmptyList<Problem>): Boolean =
    this.any { problem -> other.any { it.regel == problem.regel } }

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

        val erForhaandsgodkjent = hendelseOpplysninger.erForhaandsGodkkjent()

        val skalAvsluttePeriode = skalAvsluttePeriode(pdlEvaluering, opplysningerEvaluering, erForhaandsgodkjent)

        val slettForhaandsGodkjenning = pdlEvaluering.isRight() && opplysningerEvaluering.isLeft() && erForhaandsgodkjent

        EvalueringResultat(
            grunnlag = Pair(null, hendelseState),
            skalAvsluttePeriode,
            slettForhaandsGodkjenning
        )
    }

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
    val (_, hendelseState ) = this.first().grunnlag
    val periodeIdMap = this.associateBy { hendelseState.periodeId }
    val otherPeriodeIdMap = other.associateBy { hendelseState.periodeId }

    val periodeIder = periodeIdMap.keys + otherPeriodeIdMap.keys

    periodeIder.forEach { periodeId ->
        val result = periodeIdMap[periodeId]
        val otherResult = otherPeriodeIdMap[periodeId]

        if (result == null) {
            logger.error("Versjon 1: result is null for periodeId: $periodeId")
            return@forEach
        }

        if (otherResult == null) {
            logger.error("Versjon 2: result is null for periodeId: $periodeId")
            return@forEach
        }

        if (result.avsluttPeriode != otherResult.avsluttPeriode) {
            logger.error("AvsluttPeriode mismatch for periodeId: $periodeId")
        }

        if (result.slettForhaandsGodkjenning != otherResult.slettForhaandsGodkjenning) {
            logger.error("SlettForhaandsGodkjenning mismatch for periodeId: $periodeId")
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
