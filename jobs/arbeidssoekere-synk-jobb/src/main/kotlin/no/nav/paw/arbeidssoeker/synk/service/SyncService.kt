package no.nav.paw.arbeidssoeker.synk.service

import no.nav.paw.arbeidssoeker.synk.config.JobConfig
import no.nav.paw.arbeidssoeker.synk.utils.ArbeidssoekerCsvReader
import no.nav.paw.logging.logger.buildApplicationLogger
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

class SyncService(
    private val jobConfig: JobConfig
) {
    private val logger = buildApplicationLogger

    fun syncArbeidssoekere() {
        val file = getFile()
        val values = ArbeidssoekerCsvReader.readValues(file)
        while (values.hasNextValue()) {
            val value = values.nextValue()
            logger.debug("Prosesserer arbeidssøker {}", value.identitetsnummer)
        }
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
}