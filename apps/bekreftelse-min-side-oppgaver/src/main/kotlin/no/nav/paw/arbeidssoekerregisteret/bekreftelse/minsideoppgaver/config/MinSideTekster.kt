package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config

import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import java.net.URI

fun minSideVarselKonfigurasjon(): MinSideVarselKonfigurasjon = loadNaisOrLocalConfiguration("min_side_varsel.yaml")

data class MinSideVarselKonfigurasjon(
    val link: URI,
    val standardSpraak: Spraakkode,
    val tekster: List<MinSideTekst>
) {
    init {
        requireNotNull(tekster.find { it.spraak == standardSpraak }) {
            "Ingen av tekstene har språk $standardSpraak som er satt som standard språk"
        }
    }
}


data class Spraakkode(
    /**
     * Språkkode i henhold til ISO-639-1
     */
    val kode: String
) {
    init {
        require(kode.length == 2) { "Språkkode må være to bokstaver (ISO-639-1)" }
    }
}

data class MinSideTekst(
    val spraak: Spraakkode,
    val tekst: String
)
