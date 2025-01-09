package no.nav.paw.model

@JvmInline
value class NavIdent(val verdi: String) {
    override fun toString(): String {
        return "*".repeat(verdi.length)
    }
}