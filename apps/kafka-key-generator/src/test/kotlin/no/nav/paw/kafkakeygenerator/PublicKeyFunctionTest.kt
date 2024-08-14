package no.nav.paw.kafkakeygenerator

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.kafkakeygenerator.api.v2.PUBLIC_KEY_MODULO_VALUE
import no.nav.paw.kafkakeygenerator.api.v2.publicTopicKeyFunction

/**
 * Enkel test som vil feile ved endring av publicTopicKeyFunction eller nooen av
 * verdiene den bruker.
 * Slike endringer krever replay av eventlog til nye topics.
 */
class PublicKeyFunctionTest : FreeSpec({
    "publicTopicKeyFunction" - {
        "nøkkelen må aldri endres da dette krever replay av eventlog til nye topics" {
            val expectedModuloValue = 7_500
            PUBLIC_KEY_MODULO_VALUE shouldBe expectedModuloValue
            publicTopicKeyFunction(0) shouldBe ("internal_key_0".hashCode().toLong() % expectedModuloValue)
            publicTopicKeyFunction(expectedModuloValue.toLong()) shouldBe ("internal_key_7500".hashCode()
                .toLong() % expectedModuloValue)
            publicTopicKeyFunction(expectedModuloValue.toLong() + 1) shouldBe ("internal_key_7501".hashCode()
                .toLong() % expectedModuloValue)
            (0 until expectedModuloValue).forEach { key ->
                publicTopicKeyFunction(key.toLong()) shouldBe ("internal_key_$key".hashCode()
                    .toLong() % expectedModuloValue)
            }
        }
    }
})