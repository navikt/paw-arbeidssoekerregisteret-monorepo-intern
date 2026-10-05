package no.nav.paw.arbeidssokerregisteret.plugins

import tools.jackson.databind.cfg.DateTimeFeature

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind.module.SimpleModule
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.Detaljer
import no.nav.paw.arbeidssokerregisteret.GJELDER_FRA_DATO
import no.nav.paw.arbeidssokerregisteret.GJELDER_TIL_DATO
import no.nav.paw.arbeidssokerregisteret.PROSENT
import no.nav.paw.arbeidssokerregisteret.SISTE_ARBEIDSDAG
import no.nav.paw.arbeidssokerregisteret.SISTE_DAG_MED_LOENN
import no.nav.paw.arbeidssokerregisteret.STILLING
import no.nav.paw.arbeidssokerregisteret.STILLING_STYRK08
import java.time.LocalDate

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
            disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            addModule(SimpleModule().addDeserializer(Detaljer::class.java, DetaljerDeserializer()))
        }
    }
}

class DetaljerDeserializer : StdDeserializer<Detaljer>(Detaljer::class.java) {

    @WithSpan
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Detaljer? {
        val node: JsonNode = context.readTree(parser)
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
