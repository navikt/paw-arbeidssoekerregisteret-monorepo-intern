package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.tms.varsel.action.EksternKanal
import java.net.URI

const val MIN_SIDE_VARSEL_CONFIG = "min_side_varsel_config.yaml"

data class MinSideVarselConfig(
    val link: URI,
    val prefererteKanaler: List<EksternKanal>,
    val standardSpraak: Spraakkode,
    val tekster: List<MinSideTekst>
) {
    init {
        requireNotNull(tekster.find { it.spraak == standardSpraak }) {
            "Ingen av tekstene har språk $standardSpraak som er satt som standard språk"
        }
        require(prefererteKanaler.isNotEmpty()) { "Ingen kanaler konfigurert" }
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
