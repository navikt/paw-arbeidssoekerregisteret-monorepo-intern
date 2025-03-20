package no.nav.paw.arbeidssoeker.synk.service

import com.fasterxml.jackson.databind.MappingIterator
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoeker.synk.config.JobConfig
import no.nav.paw.arbeidssoeker.synk.consumer.InngangHttpConsumer
import no.nav.paw.arbeidssoeker.synk.model.Arbeidssoeker
import no.nav.paw.arbeidssoeker.synk.model.ArbeidssoekerFileRow
import no.nav.paw.arbeidssoeker.synk.model.asArbeidssoeker
import no.nav.paw.arbeidssoeker.synk.model.asOpprettPeriodeRequest
import no.nav.paw.arbeidssoeker.synk.model.isNotSuccess
import no.nav.paw.arbeidssoeker.synk.model.millisSince
import no.nav.paw.arbeidssoeker.synk.repository.ArbeidssoekerSynkRepository
import no.nav.paw.arbeidssoeker.synk.utils.traceAndLog
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.logging.logger.buildNamedLogger
import java.time.Instant

class ArbeidssoekerSynkService(
    private val jobConfig: JobConfig,
    private val arbeidssoekerSynkRepository: ArbeidssoekerSynkRepository,
    private val inngangHttpConsumer: InngangHttpConsumer
) {
    private val logger = buildApplicationLogger
    private val secureLogger = buildNamedLogger("secure")

    @WithSpan(value = "synkArbeidssoekere")
    fun synkArbeidssoekere(version: String, fileRows: MappingIterator<ArbeidssoekerFileRow>) {
        with(jobConfig) {
            var totalCount = 0
            val timestamp = Instant.now()
            logger.info("Starter prosessering av CSV-data")
            while (fileRows.hasNextValue()) {
                totalCount++
                if (totalCount % 100 == 0) {
                    logger.info("Prosessert {} linjer CSV-data på {} ms", totalCount, timestamp.millisSince())
                }
                val fileRow = fileRows.nextValue()
                val arbeidssoeker = fileRow.asArbeidssoeker(
                    version = version,
                    periodeTilstand = defaultVerdier.periodeTilstand,
                    forhaandsgodkjentAvAnsatt = defaultVerdier.forhaandsgodkjentAvAnsatt
                )
                prosesserArbeidssoeker(arbeidssoeker)
            }
            logger.info(
                "Fullførte prosessering av {} linjer CSV-data på {} ms",
                totalCount,
                timestamp.millisSince()
            )
        }
    }

    @WithSpan(value = "prosesserArbeidssoeker")
    @Suppress("LoggingSimilarMessage")
    private fun prosesserArbeidssoeker(arbeidssoeker: Arbeidssoeker) {
        val (version, identitetsnummer) = arbeidssoeker
        secureLogger.info("Prosesserer arbeidssøker {}", identitetsnummer)

        logger.debug("Ser etter innslag i databasen for version {}", version)
        val databaseRow = arbeidssoekerSynkRepository.find(version, identitetsnummer)
        if (databaseRow == null) {
            logger.debug("Fant ingen innslag i databasen for version {}", version)
            logger.debug("Kaller API Inngang med tilstand {}", arbeidssoeker.periodeTilstand)
            secureLogger.info(
                "Kaller API Inngang med tilstand {} for arbeidssøker {}",
                arbeidssoeker.periodeTilstand,
                identitetsnummer
            )
            val response = inngangHttpConsumer.opprettPeriode(arbeidssoeker.asOpprettPeriodeRequest())
            logger.traceAndLog(response.status)

            logger.debug("Oppretter innslag med status {} i databasen for version {}", response.status.value, version)
            arbeidssoekerSynkRepository.insert(version, identitetsnummer, response.status.value)
        } else if (databaseRow.status.isNotSuccess()) {
            logger.debug("Fant innslag med status {} i databasen for version {}", databaseRow.status, version)
            logger.debug("Kaller API Inngang med tilstand {}", arbeidssoeker.periodeTilstand)
            secureLogger.info(
                "Kaller API Inngang med tilstand {} for arbeidssøker {}",
                arbeidssoeker.periodeTilstand,
                identitetsnummer
            )
            val response = inngangHttpConsumer.opprettPeriode(arbeidssoeker.asOpprettPeriodeRequest())
            logger.traceAndLog(response.status)

            logger.debug("Oppdaterer innslag med status {} i databasen for version {}", response.status.value, version)
            arbeidssoekerSynkRepository.update(version, identitetsnummer, response.status.value)
        } else {
            logger.debug("Ignorerer fullført innslag med status {} i databasen", databaseRow.status)
            secureLogger.info("Ignorerer arbeidssøker {}", identitetsnummer)
        }
    }
}