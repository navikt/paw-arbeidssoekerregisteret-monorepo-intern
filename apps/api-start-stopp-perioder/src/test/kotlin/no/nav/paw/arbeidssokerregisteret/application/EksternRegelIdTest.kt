package no.nav.paw.arbeidssokerregisteret.application

import io.kotest.core.spec.style.FunSpec
import no.nav.paw.arbeidssokerregisteret.application.regler.ValideringsRegelId

class EksternRegelIdTest: FunSpec({
    test("verifiser at alle eksterne regler matcher en intern regel") {
        val domeneRegelIder = DomeneRegelId::class.nestedClasses
        val valideringsRegelIder = ValideringsRegelId::class.nestedClasses

    }
})
