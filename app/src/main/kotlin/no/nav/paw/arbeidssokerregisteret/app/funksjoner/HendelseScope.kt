package no.nav.paw.arbeidssokerregisteret.app.funksjoner

data class HendelseScope<A>(
    val key: A,
    val partition: Int,
    val offset: Long
)

interface HasRecordScope<A> {
    val hendelseScope: HendelseScope<A>?
}

fun <A> HendelseScope<A>.currentScope(): HendelseScope<A> = this