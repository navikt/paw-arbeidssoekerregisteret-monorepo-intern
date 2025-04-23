package no.nav.paw.kafkakeygenerator.utils

import io.ktor.server.routing.RoutingRequest
import no.nav.paw.kafkakeygenerator.vo.CallId
import java.util.*

const val TRACE_PARENT_HEADER_NAME = "traceparent"

val RoutingRequest.getCallId
    get(): CallId = headers[TRACE_PARENT_HEADER_NAME]
        ?.let { CallId(it) }
        ?: CallId(UUID.randomUUID().toString())