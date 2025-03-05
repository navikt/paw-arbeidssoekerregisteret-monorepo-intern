package no.nav.paw.arbeidssoekerregisteret.utils

import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import no.nav.paw.arbeidssoekerregisteret.model.Paging
import org.jetbrains.exposed.sql.SortOrder
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
    val size = request.queryParameters["size"]?.toIntOrNull() ?: Int.MAX_VALUE
    val offset = request.queryParameters["offset"]?.toLongOrNull() ?: 0
    val ordering = request.queryParameters["ordering"]?.let { SortOrder.valueOf(it.uppercase()) } ?: SortOrder.DESC
    return Paging(size, offset, ordering)
}

fun String.asUUID(): UUID = try {
    UUID.fromString(this)
} catch (e: IllegalArgumentException) {
    throw BadRequestException("UUID har feil format")
}
