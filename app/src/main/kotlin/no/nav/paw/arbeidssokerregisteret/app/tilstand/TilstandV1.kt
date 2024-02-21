package no.nav.paw.arbeidssokerregisteret.app.tilstand

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.HasRecordScope
import no.nav.paw.arbeidssokerregisteret.app.funksjoner.RecordScope
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.OpplysningerOmArbeidssoeker

/**
 * Denne klassen brukes til å lagre interne tilstander i arbeidssøkerregisteret.
 * Ved endringer i klassen som ikke er bakoverkompatible, må en ny versjon av klassen lages,
 * og det 'TilstandsSerde' må oppdateres for å kunne lese ny tilstand i tillegg til å kunne
 * lese den gamle tilstanden og konvertere den til ny tilstand.
 * Først når alle tilstander er oppdatert til ny versjon, kan den gamle versjonen fjernes.
 */
data class TilstandV1(
    @JsonProperty("recordScope") override val recordScope: RecordScope<Long>,
    @JsonProperty("gjeldeneTilstand") val gjeldeneTilstand: GjeldeneTilstand,
    @JsonProperty("gjeldeneIdentitetsnummer") val gjeldeneIdentitetsnummer: String,
    @JsonProperty("alleIdentitetsnummer") val alleIdentitetsnummer: Set<String>,
    @JsonProperty("gjeldenePeriode") val gjeldenePeriode: Periode?,
    @JsonProperty("forrigePeriode") val forrigePeriode: Periode?,
    @JsonProperty("sisteOpplysningerOmArbeidssoeker") val sisteOpplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker?,
    @JsonProperty("forrigeOpplysningerOmArbeidssoeker") val forrigeOpplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker?
) : HasRecordScope<Long> {
    @JsonProperty("classVersion") val classVersion: String = TilstandV1.classVersion

    companion object {
        val classVersion = "tilstand-v1"
    }
}

enum class GjeldeneTilstand {
    @JsonProperty("AVVIST") AVVIST,
    @JsonProperty("STARTET") STARTET,
    @JsonProperty("AVSLUTTET") AVSLUTTET
}