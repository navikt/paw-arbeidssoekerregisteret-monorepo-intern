package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config

import io.kotest.core.spec.style.FreeSpec

class VerifiserLastingAvTekster: FreeSpec({
    //Verifisere av vi kan laste konfig uten exceptions
    "Verifiser lasting av tekster" {
        val minSideVarselTekster = minSideVarselKonfigurasjon()
        println(minSideVarselTekster)
    }
})