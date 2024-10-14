package no.nav.paw.bekreftelse.api.utils

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig

private val objectMapper = buildObjectMapper

object JsonbSerde {
    fun serialize(data: BekreftelseTilgjengelig): String = objectMapper.writeValueAsString(data)
    fun deserialize(data: String) = objectMapper.readValue<BekreftelseTilgjengelig>(data)
}
