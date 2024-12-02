package no.nav.paw.kafkakeymaintenance.pdlprocessor.functions

import no.nav.paw.kafkakeymaintenance.vo.Data
import no.nav.paw.kafkakeymaintenance.vo.debugString
import org.slf4j.LoggerFactory

val avviksDataLogger = LoggerFactory.getLogger("avvik.data")
fun harAvvik(data: Data): Boolean =
    (data.alias
        .flatMap { it.koblinger }
        .map { it.arbeidsoekerId }
        .distinct().size > 1)
        .also { harAvvik ->
            avviksDataLogger.debug("Har avvik: {}, data: {}", harAvvik, data.debugString())
        }