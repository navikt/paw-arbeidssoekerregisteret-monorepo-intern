package no.nav.paw.arbeidssoeker.synk.service

import no.nav.paw.arbeidssoeker.synk.repository.ArbeidssoekerSynkRepository
import no.nav.paw.arbeidssoeker.synk.utils.ArbeidssoekerCsvReader
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.logging.logger.buildNamedLogger
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.io.path.name

class ArbeidssoekerSynkService(private val repository: ArbeidssoekerSynkRepository) {
    private val logger = buildApplicationLogger
    private val secureLogger = buildNamedLogger("secure")

    fun synkArbeidssoekere(path: Path) {
        var count = 0
        val timestamp = Instant.now()
        logger.info("Leser CSV-fil {} fra mappe {}", path.name, path.parent)
        val values = ArbeidssoekerCsvReader.readValues(path)
        logger.info("Starter prosessering av CSV-data")
        while (values.hasNextValue()) {
            count++
            if (count % 1000 == 0) {
                logger.info("Prosessert {} linjer CSV-data på {} ms", count, timestamp.millisSince())
            }
            val value = values.nextValue()
            secureLogger.info("Prosesserer arbeidssøker {}", value.identitetsnummer)
            val version = path.name
            val identitetsnummer = value.identitetsnummer
            val status = 200
            val row = repository.find(version, identitetsnummer)
            if (row == null) {
                repository.insert(version, identitetsnummer, status)
            } else if (row.status != 200) {
                TODO("Impl resynk av feilet rad")
            } else {
                logger.debug("Ignorerer synk av eksisterende rad")
            }
        }
        logger.info("Fullførte prosessering av {} linjer CSV-data på {} ms", count, timestamp.millisSince())
    }

    private fun Instant.millisSince(): Long = Duration.between(this, Instant.now()).toMillis()
}