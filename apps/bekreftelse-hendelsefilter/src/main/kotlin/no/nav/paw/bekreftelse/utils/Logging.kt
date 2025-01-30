package no.nav.paw.bekreftelse.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val <reified T> T.buildLogger: Logger get() = LoggerFactory.getLogger(T::class.java)

inline val buildApplicationLogger: Logger get() = LoggerFactory.getLogger("no.nav.paw.logger.application")
inline val buildErrorLogger: Logger get() = LoggerFactory.getLogger("no.nav.paw.logger.error")
