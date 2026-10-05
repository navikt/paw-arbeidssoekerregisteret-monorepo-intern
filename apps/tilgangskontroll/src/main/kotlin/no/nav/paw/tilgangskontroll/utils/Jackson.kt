package no.nav.paw.tilgangskontroll.utils

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder

val buildObjectMapper: ObjectMapper
    get() = jacksonMapperBuilder()
        .apply { configureJackson() }
        .build()

fun JsonMapper.Builder.configureJackson() {
    changeDefaultPropertyInclusion { JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL) }
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
    disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
}
