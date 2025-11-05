package no.nav.paw.felles.model

@JvmInline
value class Identitetsnummer(val value: String) {
    override fun toString(): String {
        return "*".repeat(value.length)
    }
}

fun String.asIdentitetsnummer(): Identitetsnummer = Identitetsnummer(this)
