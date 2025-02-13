package no.nav.paw.bekreftelse.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline val <reified T> T.buildLogger: Logger get() = LoggerFactory.getLogger(T::class.java)

inline val buildApplicationLogger: Logger get() = LoggerFactory.getLogger("bekreftelse.filter.application")
inline val buildClientLogger: Logger get() = LoggerFactory.getLogger("bekreftelse.filter.client")
