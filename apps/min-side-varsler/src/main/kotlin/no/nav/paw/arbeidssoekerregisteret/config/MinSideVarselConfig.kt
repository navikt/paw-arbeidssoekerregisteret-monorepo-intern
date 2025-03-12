package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.tms.varsel.action.EksternKanal
import no.nav.tms.varsel.action.EksternVarslingBestilling
import no.nav.tms.varsel.action.Sensitivitet
import java.net.URI
import java.time.Instant
import java.time.ZoneId

const val MIN_SIDE_VARSEL_CONFIG = "min_side_varsel_config.yaml"

private fun String?.isURI(): Boolean {
    return try {
        this.isNullOrBlank() || URI.create(this) != null
    } catch (e: IllegalArgumentException) {
        false
    }
}

data class MinSideVarselConfig(
    val periodeAvsluttet: MinSideVarsel,
    val bekreftelseTilgjengelig: MinSideVarsel,
    val manueltVarsel: MinSideVarsel
)

data class MinSideVarsel(
    val link: String? = null,
    val sensitivitet: VarselSensitivitet,
    val standardSpraak: Spraakkode,
    val tekster: List<VarselTekst>,
    val eksterntVarsel: EksterntVarsel?
) {
    init {
        require(link.isURI()) { "Link er ikke en URI" }
        require(tekster.isNotEmpty()) { "Ingen tekster konfigurert" }
    }
}

enum class VarselSensitivitet {
    SUBSTANTIAL,
    HIGH
}

data class EksterntVarsel(
    val preferertKanal: EksternVarselKanal,
    val smsTekst: String? = null,
    val epostTittel: String? = null,
    val epostTekst: String? = null,
    val kanBatches: Boolean = true
)

enum class EksternVarselKanal {
    SMS, EPOST, BETINGET_SMS
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
    EksternVarselKanal.BETINGET_SMS -> EksternKanal.BETINGET_SMS
}

fun EksterntVarsel.asEksternVarslingBestilling(
    utsettSendingTil: Instant? = null,
) = EksternVarslingBestilling(
    prefererteKanaler = listOf(this.preferertKanal.asEksternKanal()),
    smsVarslingstekst = this.smsTekst,
    epostVarslingstittel = this.epostTittel,
    epostVarslingstekst = this.epostTekst,
    kanBatches = this.kanBatches,
    utsettSendingTil = utsettSendingTil?.atZone(ZoneId.systemDefault())
)
