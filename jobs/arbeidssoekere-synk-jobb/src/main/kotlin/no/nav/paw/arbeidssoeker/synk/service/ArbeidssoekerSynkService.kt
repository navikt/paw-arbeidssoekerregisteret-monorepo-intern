package no.nav.paw.arbeidssoeker.synk.service

import io.ktor.http.isSuccess
import no.nav.paw.arbeidssoeker.synk.consumer.InngangHttpConsumer
import no.nav.paw.arbeidssoeker.synk.model.VersjonertArbeidssoeker
import no.nav.paw.arbeidssoeker.synk.model.asOpprettPeriodeRequest
import no.nav.paw.arbeidssoeker.synk.model.asVersioned
import no.nav.paw.arbeidssoeker.synk.model.isNotSuccess
import no.nav.paw.arbeidssoeker.synk.model.millisSince
import no.nav.paw.arbeidssoeker.synk.repository.ArbeidssoekerSynkRepository
import no.nav.paw.arbeidssoeker.synk.utils.ArbeidssoekerCsvReader
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.logging.logger.buildNamedLogger
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.name

class ArbeidssoekerSynkService(
    private val arbeidssoekerSynkRepository: ArbeidssoekerSynkRepository,
    private val inngangHttpConsumer: InngangHttpConsumer
) {
    private val logger = buildApplicationLogger
    private val secureLogger = buildNamedLogger("secure")

    fun synkArbeidssoekere(path: Path) {
        var totalCount = 0
        val timestamp = Instant.now()
        logger.info("Leser CSV-fil {} fra mappe {}", path.name, path.parent)
        val values = ArbeidssoekerCsvReader.readValues(path)
        logger.info("Starter prosessering av CSV-data")
        while (values.hasNextValue()) {
            totalCount++
            if (totalCount % 100 == 0) {
                logger.info("Prosessert {} linjer CSV-data på {} ms", totalCount, timestamp.millisSince())
            }
            val arbeidssoeker = values.nextValue()
            prosesserArbeidssoeker(arbeidssoeker.asVersioned(path.name))
        }
        logger.info("Fullførte prosessering av {} linjer CSV-data på {} ms", totalCount, timestamp.millisSince())
    }

    @Suppress("LoggingSimilarMessage")
    private fun prosesserArbeidssoeker(arbeidssoeker: VersjonertArbeidssoeker) {
        val (version, identitetsnummer) = arbeidssoeker
        secureLogger.info("Prosesserer arbeidssøker {}", identitetsnummer)

        logger.debug("Ser etter status i databasen for version {}", version)
        val row = arbeidssoekerSynkRepository.find(version, identitetsnummer)
        if (row == null) {
            logger.debug("Fant ingen status i databasen for version {}", version)
            logger.debug("Kaller API Inngang for opprettelse av periode")
            val response = inngangHttpConsumer.opprettPeriode(arbeidssoeker.asOpprettPeriodeRequest())
            if (response.status.isSuccess()) {
                logger.debug("Opprettelse av periode fullførte OK")
            } else {
                // TODO: Vurdere å ha en feilteller, og så avbryte om det blir for mange feil
                logger.debug("Opprettelse av periode feilet med status {}", response.status.value)
            }

            logger.debug("Oppretter status {} i databasen for version {}", response.status.value, version)
            arbeidssoekerSynkRepository.insert(version, identitetsnummer, response.status.value)
        } else if (row.status.isNotSuccess()) {
            logger.debug("Fant feilet status {} i databasen for version {}", row.status, version)
            logger.debug("Utfører opprettelse av periode i registeret")
            val response = inngangHttpConsumer.opprettPeriode(arbeidssoeker.asOpprettPeriodeRequest())
            if (response.status.isSuccess()) {
                logger.debug("Opprettelse av periode i registeret fullførte OK")
            } else {
                // TODO: Vurdere å ha en feilteller, og så avbryte om det blir for mange feil
                logger.debug("Opprettelse av periode i registeret feilet med status {}", response.status.value)
            }

            logger.debug("Oppdaterer status {} i databasen for version {}", response.status.value, version)
            arbeidssoekerSynkRepository.update(version, identitetsnummer, response.status.value)
        } else {
            logger.debug("Ignorerer fullført status {} i databasen", row.status)
        }
    }
}