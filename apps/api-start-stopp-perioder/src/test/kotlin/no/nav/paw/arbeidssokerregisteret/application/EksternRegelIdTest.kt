package no.nav.paw.arbeidssokerregisteret.application

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EksternRegelIdTest: FunSpec({
    test("verifiser at alle eksterne regler matcher en intern regel") {
        EksternRegelId.entries
            .filter { it != EksternRegelId.UKJENT_REGEL }
            .forEach {eksternRegelId ->
            val internRegelId = RegelId.valueOf(eksternRegelId.name)
            eksternRegelId shouldBe internRegelId.eksternRegelId
        }
    }
})
