package no.nav.paw.kafkakeygenerator.utils

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.kafkakeygenerator.model.ArbeidssoekerId

/**
 * Enkel test som vil feile ved endring av publicTopicKeyFunction eller nooen av
 * verdiene den bruker.
 * Slike endringer krever replay av eventlog til nye topics.
 */
class KeyCalculationsTest : FreeSpec({
    "publicTopicKeyFunction" - {
        "nøkkelen må aldri endres da dette krever replay av eventlog til nye topics" {
            val expectedModuloValue = 7_500
            PUBLIC_KEY_MODULO_VALUE shouldBe expectedModuloValue
            publicTopicKeyFunction(ArbeidssoekerId(0)).value shouldBe ("internal_key_0".hashCode()
                .toLong() % expectedModuloValue)
            0L.asRecordKey() shouldBe ("internal_key_0".hashCode().toLong() % expectedModuloValue)
            publicTopicKeyFunction(ArbeidssoekerId(expectedModuloValue.toLong())).value shouldBe ("internal_key_7500".hashCode()
                .toLong() % expectedModuloValue)
            expectedModuloValue.toLong().asRecordKey() shouldBe ("internal_key_7500".hashCode()
                .toLong() % expectedModuloValue)
            publicTopicKeyFunction(ArbeidssoekerId(expectedModuloValue.toLong() + 1)).value shouldBe ("internal_key_7501".hashCode()
                .toLong() % expectedModuloValue)
            (expectedModuloValue.toLong() + 1).asRecordKey() shouldBe ("internal_key_7501".hashCode()
                .toLong() % expectedModuloValue)
            (0 until expectedModuloValue).forEach { key ->
                publicTopicKeyFunction(ArbeidssoekerId(key.toLong())).value shouldBe ("internal_key_$key".hashCode()
                    .toLong() % expectedModuloValue)
            }
            (0 until expectedModuloValue).forEach { key ->
                key.toLong().asRecordKey() shouldBe ("internal_key_$key".hashCode()
                    .toLong() % expectedModuloValue)
            }
        }
    }
})