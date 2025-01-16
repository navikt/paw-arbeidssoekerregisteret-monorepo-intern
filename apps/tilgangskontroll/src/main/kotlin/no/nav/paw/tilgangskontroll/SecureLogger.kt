package no.nav.paw.tilgangskontroll

import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

object SecureLogger {
    private val secureLogger = LoggerFactory.getLogger("tjenestekall")
    private val secureMarker = MarkerFactory.getMarker("SECURE_LOG")

    fun trace(msg: String, vararg args: Any) {
        secureLogger.trace(secureMarker, msg, *args)
    }
    fun debug(msg: String, vararg args: Any) {
        secureLogger.debug(secureMarker, msg, *args)
    }
    fun info(msg: String, vararg args: Any) {
        secureLogger.info(secureMarker, msg, *args)
    }
    fun error(msg: String, vararg args: Any) {
        secureLogger.error(secureMarker, msg, *args)
    }
    fun error(msg: String, t: Throwable) {
        secureLogger.error(secureMarker, msg, t)
    }
}