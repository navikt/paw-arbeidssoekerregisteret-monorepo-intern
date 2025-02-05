package no.nav.paw.logging.logger

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val <reified T> T.buildLogger: Logger get() = LoggerFactory.getLogger(T::class.java)

fun buildNamedLogger(name: String): Logger = LoggerFactory.getLogger("no.nav.paw.logger.${name.lowercase()}")
inline val buildServerLogger: Logger get() = buildNamedLogger("server")
inline val buildApplicationLogger: Logger get() = buildNamedLogger("application")
inline val buildErrorLogger: Logger get() = buildNamedLogger("error")
