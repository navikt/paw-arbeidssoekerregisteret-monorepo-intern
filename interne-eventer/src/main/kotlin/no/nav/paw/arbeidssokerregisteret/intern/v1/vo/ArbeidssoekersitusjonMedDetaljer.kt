package no.nav.paw.arbeidssokerregisteret.intern.v1.vo

data class ArbeidssoekersitusjonMedDetaljer(
    val beskrivelse: ArbeidsoekersituasjonBeskrivelse,
    val detaljer: Map<String, String>
)