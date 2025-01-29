package no.nav.paw.dolly.api.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val buildApplicationLogger: Logger get() = LoggerFactory.getLogger("no.nav.paw.logger.application")
inline val <reified T> T.buildLogger: Logger get() = LoggerFactory.getLogger(T::class.java)