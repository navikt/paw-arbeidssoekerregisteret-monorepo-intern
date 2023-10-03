package no.nav.paw.arbeidssokerregisteret.domain

@JvmInline
value class Foedselsnummer(val verdi: String) {
    override fun toString(): String {
        return "*".repeat(11)
    }
}
