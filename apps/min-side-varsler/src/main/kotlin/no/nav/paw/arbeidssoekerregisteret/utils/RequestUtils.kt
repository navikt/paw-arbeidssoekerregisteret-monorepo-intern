package no.nav.paw.arbeidssoekerregisteret.utils

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import no.nav.paw.arbeidssoekerregisteret.model.Order
import no.nav.paw.arbeidssoekerregisteret.model.Paging
import java.util.*

fun ApplicationCall.pathVarselId(): UUID {
    return parameters["varselId"]?.asUUID() ?: throw BadRequestException("Forespørsel mangler varselId")
}

fun ApplicationCall.pathBestillingId(): UUID {
    return parameters["bestillingId"]?.asUUID() ?: throw BadRequestException("Forespørsel mangler bestillingId")
}

fun ApplicationCall.queryPeriodeId(): UUID {
    return request.queryParameters["periodeId"]?.asUUID() ?: throw BadRequestException("Forespørsel mangler periodeId")
}

fun ApplicationCall.getPaging(): Paging {
    val offset = request.queryParameters["offset"]?.toLongOrNull() ?: 0
    val size = request.queryParameters["size"]?.toIntOrNull() ?: Int.MAX_VALUE
    val order = request.queryParameters["order"]?.let { Order.valueOf(it.uppercase()) } ?: Order.DESC
    return Paging.of(offset, size, order)
}

fun String.asUUID(): UUID = try {
    UUID.fromString(this)
} catch (e: IllegalArgumentException) {
    throw BadRequestException("UUID har feil format")
}
