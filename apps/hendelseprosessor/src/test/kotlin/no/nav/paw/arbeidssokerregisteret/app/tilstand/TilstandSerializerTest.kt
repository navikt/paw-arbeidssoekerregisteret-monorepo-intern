package no.nav.paw.arbeidssokerregisteret.app.tilstand

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull

class TilstandSerializerTest: FreeSpec({
    "Tilstandsserialisering".config(enabled = true) - {
        val serde = TilstandSerde()
        "Verifiser serializering av null" {
            val tilstand: TilstandV1? = null
            val resultat = serde.serializer().serialize("topic", tilstand)
            withClue("Forventet null, men fikk: ${resultat?.size} bytes, bytes=${resultat?.toList()?.joinToString(", ")}") {
                resultat.shouldBeNull()
            }
        }
    }
})