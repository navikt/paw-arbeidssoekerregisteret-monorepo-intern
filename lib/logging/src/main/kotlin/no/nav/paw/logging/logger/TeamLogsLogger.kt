package no.nav.paw.logging.logger

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.Marker
import org.slf4j.MarkerFactory

object TeamLogsLogger : Logger {
    private val logger = LoggerFactory.getLogger("team-logs-logger")
    private val defaultMarker = MarkerFactory.getMarker("TEAM_LOGS")

    override fun getName(): String = logger.name

    // TRACE
    override fun isTraceEnabled(): Boolean = logger.isTraceEnabled(defaultMarker)
    override fun isTraceEnabled(marker: Marker): Boolean = logger.isTraceEnabled(marker)
    override fun trace(msg: String) = logger.trace(defaultMarker, msg)
    override fun trace(msg: String, arg: Any) = logger.trace(defaultMarker, msg, arg)
    override fun trace(msg: String, arg1: Any, arg2: Any) = logger.trace(defaultMarker, msg, arg1, arg2)
    override fun trace(msg: String, vararg args: Any) = logger.trace(defaultMarker, msg, *args)
    override fun trace(msg: String, t: Throwable) = logger.trace(defaultMarker, msg, t)
    override fun trace(marker: Marker, msg: String) = logger.trace(marker, msg)
    override fun trace(marker: Marker, msg: String, arg: Any) = logger.trace(marker, msg, arg)
    override fun trace(marker: Marker, msg: String, arg1: Any, arg2: Any) = logger.trace(marker, msg, arg1, arg2)
    override fun trace(marker: Marker, msg: String, vararg args: Any) = logger.trace(marker, msg, *args)
    override fun trace(marker: Marker, msg: String, t: Throwable) = logger.trace(marker, msg, t)

    // DEBUG
    override fun isDebugEnabled(): Boolean = logger.isDebugEnabled(defaultMarker)
    override fun isDebugEnabled(marker: Marker): Boolean = logger.isDebugEnabled(marker)
    override fun debug(msg: String) = logger.debug(defaultMarker, msg)
    override fun debug(msg: String, arg: Any) = logger.debug(defaultMarker, msg, arg)
    override fun debug(msg: String, arg1: Any, arg2: Any) = logger.debug(defaultMarker, msg, arg1, arg2)
    override fun debug(msg: String, vararg args: Any) = logger.debug(defaultMarker, msg, *args)
    override fun debug(msg: String, t: Throwable) = logger.debug(defaultMarker, msg, t)
    override fun debug(marker: Marker, msg: String) = logger.debug(marker, msg)
    override fun debug(marker: Marker, msg: String, arg: Any) = logger.debug(marker, msg, arg)
    override fun debug(marker: Marker, msg: String, arg1: Any, arg2: Any) = logger.debug(marker, msg, arg1, arg2)
    override fun debug(marker: Marker, msg: String, vararg args: Any) = logger.debug(marker, msg, *args)
    override fun debug(marker: Marker, msg: String, t: Throwable) = logger.debug(marker, msg, t)

    // INFO
    override fun isInfoEnabled(): Boolean = logger.isInfoEnabled(defaultMarker)
    override fun isInfoEnabled(marker: Marker): Boolean = logger.isInfoEnabled(marker)
    override fun info(msg: String) = logger.info(defaultMarker, msg)
    override fun info(msg: String, arg: Any) = logger.info(defaultMarker, msg, arg)
    override fun info(msg: String, arg1: Any, arg2: Any) = logger.info(defaultMarker, msg, arg1, arg2)
    override fun info(msg: String, vararg args: Any) = logger.info(defaultMarker, msg, *args)
    override fun info(msg: String, t: Throwable) = logger.info(defaultMarker, msg, t)
    override fun info(marker: Marker, msg: String) = logger.info(marker, msg)
    override fun info(marker: Marker, msg: String, arg: Any) = logger.info(marker, msg, arg)
    override fun info(marker: Marker, msg: String, arg1: Any, arg2: Any) = logger.info(marker, msg, arg1, arg2)
    override fun info(marker: Marker, msg: String, vararg args: Any) = logger.info(marker, msg, *args)
    override fun info(marker: Marker, msg: String, t: Throwable) = logger.info(marker, msg, t)

    // WARN
    override fun isWarnEnabled(): Boolean = logger.isWarnEnabled(defaultMarker)
    override fun isWarnEnabled(marker: Marker): Boolean = logger.isWarnEnabled(marker)
    override fun warn(msg: String) = logger.warn(defaultMarker, msg)
    override fun warn(msg: String, arg: Any) = logger.warn(defaultMarker, msg, arg)
    override fun warn(msg: String, arg1: Any, arg2: Any) = logger.warn(defaultMarker, msg, arg1, arg2)
    override fun warn(msg: String, vararg args: Any) = logger.warn(defaultMarker, msg, *args)
    override fun warn(msg: String, t: Throwable) = logger.warn(defaultMarker, msg, t)
    override fun warn(marker: Marker, msg: String) = logger.warn(marker, msg)
    override fun warn(marker: Marker, msg: String, arg: Any) = logger.warn(marker, msg, arg)
    override fun warn(marker: Marker, msg: String, arg1: Any, arg2: Any) = logger.warn(marker, msg, arg1, arg2)
    override fun warn(marker: Marker, msg: String, vararg args: Any) = logger.warn(marker, msg, *args)
    override fun warn(marker: Marker, msg: String, t: Throwable) = logger.warn(marker, msg, t)

    // ERROR
    override fun isErrorEnabled(): Boolean = logger.isErrorEnabled(defaultMarker)
    override fun isErrorEnabled(marker: Marker): Boolean = logger.isErrorEnabled(marker)
    override fun error(msg: String) = logger.error(defaultMarker, msg)
    override fun error(msg: String, arg: Any) = logger.error(defaultMarker, msg, arg)
    override fun error(msg: String, arg1: Any, arg2: Any) = logger.error(defaultMarker, msg, arg1, arg2)
    override fun error(msg: String, vararg args: Any) = logger.error(defaultMarker, msg, *args)
    override fun error(msg: String, t: Throwable) = logger.error(defaultMarker, msg, t)
    override fun error(marker: Marker, msg: String) = logger.error(marker, msg)
    override fun error(marker: Marker, msg: String, arg: Any) = logger.error(marker, msg, arg)
    override fun error(marker: Marker, msg: String, arg1: Any, arg2: Any) = logger.error(marker, msg, arg1, arg2)
    override fun error(marker: Marker, msg: String, vararg args: Any) = logger.error(marker, msg, *args)
    override fun error(marker: Marker, msg: String, t: Throwable) = logger.error(marker, msg, t)
}