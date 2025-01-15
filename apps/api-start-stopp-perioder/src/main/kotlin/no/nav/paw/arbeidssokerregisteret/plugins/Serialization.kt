package no.nav.paw.arbeidssokerregisteret.plugins

import arrow.integrations.jackson.module.NonEmptyListModule
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.Detaljer
import no.nav.paw.arbeidssokerregisteret.*
import java.time.LocalDate

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            registerModule(JavaTimeModule())
            registerKotlinModule()
            registerModule(NonEmptyListModule)
            registerModule(SimpleModule().addDeserializer(Detaljer::class.java, DetaljerDeserializer()))
        }
    }
}

class DetaljerDeserializer : StdDeserializer<Detaljer>(null as Class<Detaljer>?) {

    @WithSpan
    override fun deserialize(parser: JsonParser?, context: DeserializationContext?): Detaljer? {
        if (parser == null) return null
        if (context == null) return null
        val node = parser.codec.readTree<JsonNode>(parser)
        return Detaljer(
            gjelderFraDatoIso8601 = node.get(GJELDER_FRA_DATO)
                ?.asText()
                ?.let(LocalDate::parse),
            gjelderTilDatoIso8601 = node.get(GJELDER_TIL_DATO)
                ?.asText()
                ?.let(LocalDate::parse),
            stillingStyrk08 = node.get(STILLING_STYRK08)?.asText(),
            stilling = node.get(STILLING)?.asText(),
            sisteDagMedLoennIso8601 = node.get(SISTE_DAG_MED_LOENN)
                ?.asText()
                ?.let(LocalDate::parse),
            sisteArbeidsdagIso8601 = node.get(SISTE_ARBEIDSDAG)
                ?.asText()
                ?.let(LocalDate::parse),
            prosent = node.get(PROSENT)?.asText()
        )
    }

}
