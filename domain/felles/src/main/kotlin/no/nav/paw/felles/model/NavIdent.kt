package no.nav.paw.felles.model

@JvmInline
value class NavIdent(val verdi: String) {
    override fun toString(): String {
        return "*".repeat(verdi.length)
    }
}

fun String.asNavIdent(): NavIdent = NavIdent(this)