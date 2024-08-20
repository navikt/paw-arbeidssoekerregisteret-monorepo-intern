package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka

import arrow.core.Either
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.ApplicationInfo
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.clients.pdl.PdlHentForenkletStatus
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.clients.pdl.PdlHentForenkletStatus.Companion.logger
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.clients.pdl.PdlHentPerson
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.serdes.HendelseState
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.metrics.tellPdlAvsluttetHendelser
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.metrics.tellStatusFraPdlHentPersonBolk
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils.genererPersonFakta
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils.negativeOpplysninger
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils.statusToOpplysningMap
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils.toAarsak
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils.toPerson
import no.nav.paw.arbeidssokerregisteret.application.OK
import no.nav.paw.arbeidssokerregisteret.application.Problem
import no.nav.paw.arbeidssokerregisteret.application.evaluer
import no.nav.paw.arbeidssokerregisteret.application.hendelseOpplysningTilDomeneOpplysninger
import no.nav.paw.arbeidssokerregisteret.application.reglerForInngangIPrioritertRekkefolge
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.pdl.graphql.generated.hentforenkletstatusbolk.Folkeregisterpersonstatus
import no.nav.paw.pdl.graphql.generated.hentforenkletstatusbolk.HentPersonBolkResult as ForenkletStatusBolkResult
import no.nav.paw.pdl.graphql.generated.hentpersonbolk.HentPersonBolkResult
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
                if (pdlHentPersonResults == null) {
                    logger.warn("PDL hentPersonBolk returnerte null")
                } else {
                    pdlHentPersonResults.processResultsV2(
                        chunk,
                        logger
                    )
                }

                // Versjon 1
                val pdlResults = hentForenkletStatus(identitetsnummere, pdlHentForenkletStatus)
                if (pdlResults == null) {
                    logger.error("PDL hentForenkletStatus returnerte null")
                    return@chunked
                }

                pdlResults.processResults(
                    chunk,
                    hendelseStateStore,
                    ctx,
                    prometheusMeterRegistry,
                    logger
                )
            }
    }
}


private fun List<ForenkletStatusBolkResult>.processResults(
    chunk: List<KeyValue<UUID, HendelseState>>,
    hendelseStateStore: KeyValueStore<UUID, HendelseState>,
    ctx: ProcessorContext<Long, Hendelse>,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    logger: Logger
) = this.forEachIndexed { _, result ->
    prometheusMeterRegistry.tellStatusFraPdlHentPersonBolk(result.code)

    if (result.code in pdlErrorResponses) {
        logger.error("Feil ved henting av forenklet status fra PDL: ${result.code}")
        return@forEachIndexed
    }

    val (folkeregisterpersonstatus, hendelseState) = hentPersonStatusOgHendelseState(result, chunk)
        ?: return@forEachIndexed

    if (folkeregisterpersonstatus.erBosattEtterFolkeregisterloven) {
        oppdaterHendelseStateOpplysninger(hendelseState, hendelseStateStore)
        return@forEachIndexed
    }

    logger.info("Vurderer grunnlag for utmelding av person med folkeregisterstatus: $folkeregisterpersonstatus og opplysninger fra hendelse: ${hendelseState.opplysninger}")

    val aarsak = folkeregisterpersonstatus
        .filterAvsluttPeriodeGrunnlag(hendelseState.opplysninger)
        .ifEmpty {
            logger.info("Versjon 1: OK, matchende opplysning fra startet hendelse og forhåndsgodkjent av ansatt")
            return@forEachIndexed
        }
        .toAarsak()

    logger.info("Versjon 2: PROBLEM, negative opplysninger fra pdl: ${folkeregisterpersonstatus.filterAvsluttPeriodeGrunnlag(hendelseState.opplysninger)}, generert aarsak: $aarsak")

    val avsluttetHendelse = genererAvsluttetHendelseRecord(hendelseState, aarsak)

    logger.info("Sender avsluttet hendelse med aarsak: $aarsak")

    ctx.forward(avsluttetHendelse)
        .also {
            prometheusMeterRegistry.tellPdlAvsluttetHendelser(aarsak)
            hendelseStateStore.delete(hendelseState.periodeId)
        }
}

private fun List<HentPersonBolkResult>.processResultsV2(
    chunk: List<KeyValue<UUID, HendelseState>>,
    logger: Logger
) = this.forEach { result ->
    if (result.code in pdlErrorResponses) {
        logger.error("Versjon 2: Feil ved henting av Person fra PDL: ${result.code}")
        return@forEach
    }

    val person = result.person ?: throw IllegalStateException("Versjon 2: Person mangler")
    val hendelseOpplysninger = chunk.find { it.value.identitetsnummer == result.ident }
        ?.value?.opplysninger ?: throw IllegalStateException("Versjon 2: HendelseState mangler")

    val domeneOpplysninger = hendelseOpplysninger
        .filterNot { it == Opplysning.FORHAANDSGODKJENT_AV_ANSATT }
        .map { hendelseOpplysningTilDomeneOpplysninger(it) as no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning }
        .toSet()

    val opplysningerEvaluering = reglerForInngangIPrioritertRekkefolge.evaluer(domeneOpplysninger)
    val pdlEvaluering = reglerForInngangIPrioritertRekkefolge.evaluer(genererPersonFakta(person.toPerson()))

    val erForhaandsgodkjent = hendelseOpplysninger.contains(Opplysning.FORHAANDSGODKJENT_AV_ANSATT)

    when {
        pdlEvaluering.isLeft() -> handleLeftEvaluation(
            pdlEvaluering, opplysningerEvaluering, erForhaandsgodkjent, logger
        )
        pdlEvaluering.isRight() -> handleRightEvaluation(
            opplysningerEvaluering, erForhaandsgodkjent, logger
        )
    }
}

private fun handleLeftEvaluation(
    pdlEvaluering: Either<Problem, OK>,
    opplysningerEvaluering: Either<Problem, OK>,
    erForhaandsgodkjent: Boolean,
    logger: Logger
) {
    val pdlEvalueringResultat = pdlEvaluering.leftOrNull()?.opplysning?.toSet()
        ?: throw IllegalStateException("Versjon 2: PDL evaluering mangler opplysning")
    val opplysningerEvalueringResultat = opplysningerEvaluering.leftOrNull()?.opplysning?.toSet()
        ?: throw IllegalStateException("Versjon 2: Opplysninger evaluering mangler opplysning")

    if (pdlEvalueringResultat == opplysningerEvalueringResultat && erForhaandsgodkjent) {
        logger.info("Versjon 2: OK, matchende opplysning fra startet hendelse og forhåndsgodkjent av ansatt: $pdlEvalueringResultat")
    } else {
        val aarsak = pdlEvalueringResultat.filterNot { it in opplysningerEvalueringResultat }.toAarsak()
        logger.info("Versjon 2: PROBLEM, negative opplysninger fra pdl: $pdlEvalueringResultat, generert aarsak: $aarsak")
    }
}

private fun handleRightEvaluation(
    opplysningerEvaluering: Either<Problem, OK>,
    erForhaandsgodkjent: Boolean,
    logger: Logger
) {
    if (opplysningerEvaluering.isLeft() && erForhaandsgodkjent) {
        logger.info("Versjon 2: OK, ingen negative opplysninger fra pdl, negative opplysninger fra startet hendelse og forhåndsgodkjent av ansatt funnet")
    } else {
        logger.info("Versjon 2: OK, ingen negative opplysninger fra pdl")
    }
}


private fun hentPersonStatusOgHendelseState(
    result: ForenkletStatusBolkResult,
    chunk: List<KeyValue<UUID, HendelseState>>
): Pair<List<Folkeregisterpersonstatus>, HendelseState>? {
    val person = result.person ?: return null
    val hendelseState = chunk.find { it.value.identitetsnummer == result.ident }
        ?.value ?: return null

    return Pair(person.folkeregisterpersonstatus, hendelseState)
}

private fun oppdaterHendelseStateOpplysninger(
    hendelseState: HendelseState,
    hendelseStateStore: KeyValueStore<UUID, HendelseState>
) {
    if (Opplysning.FORHAANDSGODKJENT_AV_ANSATT in hendelseState.opplysninger && hendelseState.opplysninger.any { it in negativeOpplysninger }) {
        val oppdatertHendelseState = hendelseState.copy(
            opplysninger = hendelseState.opplysninger
                .filterNot { it == Opplysning.FORHAANDSGODKJENT_AV_ANSATT || it in negativeOpplysninger }
                .toSet()
        )
        hendelseStateStore.put(hendelseState.periodeId, oppdatertHendelseState)
        logger.info("Versjon 1: OK, ingen negative opplysninger fra pdl, negative opplysninger fra startet hendelse og forhåndsgodkjent av ansatt funnet")
    } else {
        logger.info("Versjon 1: OK, ingen negative opplysninger fra pdl")
    }
}

private fun hentForenkletStatus(
    identitetsnummere: List<String>,
    pdlHentForenkletStatus: PdlHentForenkletStatus,
): List<ForenkletStatusBolkResult>? {
    return pdlHentForenkletStatus.hentForenkletStatus(
        identitetsnummere,
        UUID.randomUUID().toString(),
        "paw-arbeidssoekerregisteret-utgang-pdl",
    )
}

private fun hentPersonBolk(
    identitetsnummere: List<String>,
    pdlHentPersonBolk: PdlHentPerson,
): List<HentPersonBolkResult>? {
    return pdlHentPersonBolk.hentPerson(
        identitetsnummere,
        UUID.randomUUID().toString(),
        "paw-arbeidssoekerregisteret-utgang-pdl",
    )
}

private fun List<KeyValue<UUID, HendelseState>>.filterValidHendelseStates(): List<KeyValue<UUID, HendelseState>> =
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



private val List<Folkeregisterpersonstatus>.erBosattEtterFolkeregisterloven
    get(): Boolean =
        this.any { it.forenkletStatus == "bosattEtterFolkeregisterloven" }

private val pdlErrorResponses = setOf(
    "bad_request",
    "not_found"
)

private fun genererAvsluttetHendelseRecord(
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
