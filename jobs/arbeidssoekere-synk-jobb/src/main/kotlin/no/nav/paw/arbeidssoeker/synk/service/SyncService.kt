package no.nav.paw.arbeidssoeker.synk.service

import no.nav.paw.arbeidssoeker.synk.config.JobConfig
import no.nav.paw.arbeidssoeker.synk.utils.ArbeidssoekerCsvReader
import no.nav.paw.logging.logger.buildApplicationLogger
import no.nav.paw.logging.logger.buildNamedLogger
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import kotlin.io.path.Path

class SyncService(
    private val jobConfig: JobConfig
) {
    private val logger = buildApplicationLogger
    private val secureLogger = buildNamedLogger("secure")

    fun syncArbeidssoekere() {
        var count = 0
        val timestamp = Instant.now()
        val file = getFile()
        logger.info("Leser CSV-fil fra {}", file.absolutePath)
        val values = ArbeidssoekerCsvReader.readValues(file)
        logger.info("Starter prosessering av CSV-data")
        while (values.hasNextValue()) {
            count++
            if (count % 1000 == 0) {
                logger.info("Prosessert {} linjer CSV-data på {} ms", count, timestamp.millisSince())
            }
            val value = values.nextValue()
            secureLogger.info("Prosesserer arbeidssøker {}", value.identitetsnummer)
        }
        logger.info("Fullførte prosessering av {} linjer CSV-data på {} ms", count, timestamp.millisSince())
    }

    private fun getFile(): File {
        with(jobConfig) {
            val path = Path(mountPath)
            if (!Files.exists(path)) {
                throw IllegalStateException("$mountPath ikke funnet")
            }
            if (!Files.isRegularFile(path)) {
                throw IllegalStateException("$mountPath er ikke en fil")
            }
            if (!Files.isReadable(path)) {
                throw IllegalStateException("$mountPath kan ikke leses fra")
            }
            return path.toFile()
        }
    }

    private fun Instant.millisSince(): Long = Duration.between(this, Instant.now()).toMillis()
}