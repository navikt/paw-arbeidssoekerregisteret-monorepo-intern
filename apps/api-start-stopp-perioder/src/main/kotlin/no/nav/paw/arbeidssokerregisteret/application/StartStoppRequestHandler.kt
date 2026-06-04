package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.RegelEvalResultat
import no.nav.paw.felles.collection.PawNonEmptyList
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.kafka.producer.sendDeferred
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class StartStoppRequestHandler(
    private val hendelseTopic: String,
    private val requestValidator: RequestValidator,
    private val producer: Producer<Long, Hendelse>,
    private val kafkaKeysClient: KafkaKeysClient
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @WithSpan
    suspend fun startArbeidssokerperiode(
        requestScope: RequestScope,
        identitetsnummer: Identitetsnummer,
        erForhaandsGodkjentAvVeileder: Boolean,
        feilretting: Feilretting?
    ): Either<PawNonEmptyList<Problem>, GrunnlagForGodkjenning> =
        coroutineScope {
            val kafkaKeysResponse = async { kafkaKeysClient.getIdAndKey(identitetsnummer.value) }
            val resultat = requestValidator.validerStartAvPeriodeOenske(
                requestScope = requestScope,
                identitetsnummer = identitetsnummer,
                erForhaandsGodkjentAvVeileder = erForhaandsGodkjentAvVeileder,
                feilretting = feilretting
            )
            val (id, key) = kafkaKeysResponse.await()
            val hendelse = somHendelse(
                requestScope = requestScope,
                id = id,
                identitetsnummer = identitetsnummer,
                resultat = resultat,
                feilretting = feilretting
            )
            val record = ProducerRecord(
                hendelseTopic,
                key,
                hendelse
            )
            producer.sendDeferred(record).await()
            resultat
        }

    @WithSpan
    suspend fun avsluttArbeidssokerperiode(
        requestScope: RequestScope,
        identitetsnummer: Identitetsnummer,
        feilretting: Feilretting?
    ): Either<PawNonEmptyList<Problem>, GrunnlagForGodkjenning> {
        val (id, key) = kafkaKeysClient.getIdAndKey(identitetsnummer.value)
        val tilgangskontrollResultat = requestValidator.validerRequest(
            requestScope = requestScope,
            identitetsnummer = identitetsnummer,
            feilretting = feilretting
        )

        val regelEvalResultat = try {
            if (feilretting?.tidspunktFraKilde?.avviksType == AvviksType.SLETTET) {
                RegelEvalResultat.IKKE_RELEVANT
            } else {
                requestValidator.validerStartAvPeriodeOenske(
                    requestScope = requestScope,
                    identitetsnummer = identitetsnummer,
                    feilretting = null
                ).fold(
                    ifLeft = {
                        evaluerRegelsett(it.first)
                    },
                    ifRight = {
                        RegelEvalResultat.OK
                    }
                )
            }
        } catch (e: Exception) {
            logger.error("Feil under validering av start av periodeønske", e)
            RegelEvalResultat.FEIL_UNDER_EVAL
        }

        val hendelse = stoppResultatSomHendelse(
            requestScope = requestScope,
            id = id,
            identitetsnummer = identitetsnummer,
            resultat = tilgangskontrollResultat,
            feilretting = feilretting,
            regelEvalResultat = regelEvalResultat
        )
        val record = ProducerRecord(
            hendelseTopic,
            key,
            hendelse
        )
        val recordMetadata = producer.sendDeferred(record).await()
        logger.trace("Sendte melding til kafka: type={}, offset={}", hendelse.hendelseType, recordMetadata.offset())
        return tilgangskontrollResultat
    }

    suspend fun kanRegistreresSomArbeidssoker(
        requestScope: RequestScope,
        identitetsnummer: Identitetsnummer
    ): Either<PawNonEmptyList<Problem>, GrunnlagForGodkjenning> {
        val (id, key) = kafkaKeysClient.getIdAndKey(identitetsnummer.value)
        val resultat = requestValidator.validerStartAvPeriodeOenske(
            requestScope = requestScope,
            identitetsnummer = identitetsnummer,
            feilretting = null
        )
        if (resultat.isLeft()) {
            val hendelse = somHendelse(
                requestScope = requestScope,
                id = id,
                identitetsnummer = identitetsnummer,
                resultat = resultat, feilretting = null
            )
            val record = ProducerRecord(
                hendelseTopic,
                key,
                hendelse
            )
            val recordMetadata = producer.sendDeferred(record).await()
            logger.trace("Sendte melding til kafka: type={}, offset={}", hendelse.hendelseType, recordMetadata.offset())
        }
        return resultat
    }
}

fun evaluerRegelsett(problem: Problem):RegelEvalResultat {
    val regelId = problem.regel.id
    if (regelId !is DomeneRegelId) return (RegelEvalResultat.UDEFINERT)
    val regelEvalResultat = when (regelId) {
        Doed -> RegelEvalResultat.DOED
        ErStatsborgerILandMedAvtale -> RegelEvalResultat.UDEFINERT
        EuEoesStatsborgerMenHarStatusIkkeBosatt -> RegelEvalResultat.EU_EOES_IKKE_BOSATT
        EuEoesStatsborgerOver18Aar -> RegelEvalResultat.UDEFINERT
        ForhaandsgodkjentAvAnsatt -> RegelEvalResultat.UDEFINERT
        IkkeBosattINorgeIHenholdTilFolkeregisterloven -> if (problem.opplysninger.contains(DomeneOpplysning.ErNorskStatsborger)) {
            RegelEvalResultat.NORSK_IKKE_BOSATT
        } else {
            RegelEvalResultat.IKKE_BOSATT
        }
        IkkeFunnet -> RegelEvalResultat.UDEFINERT
        Opphoert -> RegelEvalResultat.OPPHOERT
        Over18AarOgBosattEtterFregLoven -> RegelEvalResultat.UDEFINERT
        Savnet -> RegelEvalResultat.SAVNET
        UkjentAlder -> RegelEvalResultat.UDEFINERT
        Under18Aar -> RegelEvalResultat.UDEFINERT
    }
    return regelEvalResultat
}
