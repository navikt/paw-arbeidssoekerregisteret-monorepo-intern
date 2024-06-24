package no.nav.paw.arbeidssokerregisteret.application

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.application.regler.AuthRegelId

class EksternRegelIdTest: FunSpec({
    test("verifiser at alle eksterne regler matcher en intern regel") {
        val domeneRegelIder = DomeneRegelId::class.nestedClasses
        val authRegelIder = AuthRegelId::class.nestedClasses

    }
})
