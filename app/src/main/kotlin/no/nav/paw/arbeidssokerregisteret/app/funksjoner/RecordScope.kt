package no.nav.paw.arbeidssokerregisteret.app.funksjoner

data class RecordScope<A>(
    val key: A,
    val partition: Int,
    val offset: Long
)

interface HasRecordScope<A> {
    val recordScope: RecordScope<A>?
}

fun <A> RecordScope<A>.currentScope(): RecordScope<A> = this