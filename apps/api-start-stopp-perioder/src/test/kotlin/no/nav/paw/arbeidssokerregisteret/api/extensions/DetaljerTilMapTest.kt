package no.nav.paw.arbeidssokerregisteret.api.extensions

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.Detaljer
import no.nav.paw.arbeidssokerregisteret.plugins.DetaljerDeserializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.readValue

/**
 * Detaljer blir til Map<String, String> i opplysningshendelsen og videre til Avro.
 * Verdiene "null" og "" skal behandles som manglende verdi og ikke tas med i kartet.
 * Jackson 2 leste JSON-null som "null", Jackson 3 leser det som "".
 */
class DetaljerTilMapTest : FreeSpec({
    val objectMapper = JsonMapper.builder()
        .addModule(SimpleModule().addDeserializer(Detaljer::class.java, DetaljerDeserializer()))
        .build()

    fun detaljerSomMap(json: String): Map<String, String> =
        objectMapper.readValue<Detaljer>(json).toInternalApi()

    "JSON-null i tekstfeltene tas ikke med" {
        detaljerSomMap("""{"stilling":null,"stilling_styrk08":null,"prosent":null}""") shouldBe emptyMap()
    }

    "tom tekst i tekstfeltene tas ikke med" {
        detaljerSomMap("""{"stilling":"","stilling_styrk08":"","prosent":""}""") shouldBe emptyMap()
    }

    "teksten \"null\" i tekstfeltene tas ikke med" {
        detaljerSomMap("""{"stilling":"null","stilling_styrk08":"null","prosent":"null"}""") shouldBe emptyMap()
    }

    "gyldige verdier tas med uendret" {
        detaljerSomMap(
            """
            {
              "stilling":"Kokk",
              "stilling_styrk08":"5120",
              "prosent":"50",
              "gjelder_fra_dato_iso8601":"2025-01-01",
              "gjelder_til_dato_iso8601":"2025-02-01",
              "siste_dag_med_loenn_iso8601":"2025-01-15",
              "siste_arbeidsdag_iso8601":"2025-01-14"
            }
            """
        ) shouldBe mapOf(
            "stilling" to "Kokk",
            "stilling_styrk08" to "5120",
            "prosent" to "50",
            "gjelder_fra_dato_iso8601" to "2025-01-01",
            "gjelder_til_dato_iso8601" to "2025-02-01",
            "siste_dag_med_loenn_iso8601" to "2025-01-15",
            "siste_arbeidsdag_iso8601" to "2025-01-14"
        )
    }

    "en blanding beholder bare gyldige verdier" {
        detaljerSomMap("""{"stilling":"Kokk","stilling_styrk08":"null","prosent":""}""") shouldBe
            mapOf("stilling" to "Kokk")
    }
})
