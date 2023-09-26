package no.nav.paw.arbeidssokerregisteret.arbeidssoker.domain

@JvmInline
value class Foedselsnummer(val verdi: String) {
    override fun toString(): String {
        return "*".repeat(11)
    }
}
