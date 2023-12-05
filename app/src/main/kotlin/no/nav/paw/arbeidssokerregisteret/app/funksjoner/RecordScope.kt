package no.nav.paw.arbeidssokerregisteret.app.funksjoner

data class RecordScope<A>(
    val key: A,
    val partition: Int,
    val offset: Long
)

fun <A> RecordScope<A>.currentScope() = this