package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.EksternVarslingBestilling
import no.nav.tms.varsel.action.Sensitivitet
import java.net.URI
import java.time.Instant
import java.time.ZoneId

const val MIN_SIDE_VARSEL_CONFIG = "min_side_varsel_config.yaml"

data class MinSideVarselConfig(
    val periodeAvsluttet: MinSideVarsel,
    val bekreftelseTilgjengelig: MinSideVarsel
)

data class MinSideVarsel(
    val link: URI,
    val sensitivitet: VarselSensitivitet,
    val standardSpraak: Spraakkode,
    val tekster: List<VarselTekst>,
    val eksterntVarsel: EksterntVarsel?
) {
    init {
        require(tekster.isNotEmpty()) { "Ingen tekster konfigurert" }
    }
}

enum class VarselSensitivitet {
    SUBSTANTIAL,
    HIGH
}

data class EksterntVarsel(
    val prefererteKanaler: List<EksternVarselKanal>,
    val kanBatches: Boolean = true
) {
    init {
        require(prefererteKanaler.isNotEmpty()) { "Ingen kanaler konfigurert" }
    }
}

enum class EksternVarselKanal {
    SMS, EPOST
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

data class VarselTekst(
    val spraak: Spraakkode,
    val tekst: String
)

fun VarselSensitivitet.asSensitivitet() = when (this) {
    VarselSensitivitet.SUBSTANTIAL -> Sensitivitet.Substantial
    VarselSensitivitet.HIGH -> Sensitivitet.High
}

fun EksternVarselKanal.asEksternKanal() = when (this) {
    EksternVarselKanal.SMS -> EksternKanal.SMS
    EksternVarselKanal.EPOST -> EksternKanal.EPOST
}

fun EksterntVarsel.asEksternVarslingBestilling(
    utsettSendingTil: Instant? = null,
) = EksternVarslingBestilling(
    prefererteKanaler = this.prefererteKanaler.map { it.asEksternKanal() },
    kanBatches = this.kanBatches,
    utsettSendingTil = utsettSendingTil?.atZone(ZoneId.systemDefault())
)
