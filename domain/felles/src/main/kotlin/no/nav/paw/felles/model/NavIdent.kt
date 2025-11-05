package no.nav.paw.felles.model

@JvmInline
value class NavIdent(val value: String) {
    override fun toString(): String {
        return "*".repeat(value.length)
    }
}

fun String.asNavIdent(): NavIdent = NavIdent(this)