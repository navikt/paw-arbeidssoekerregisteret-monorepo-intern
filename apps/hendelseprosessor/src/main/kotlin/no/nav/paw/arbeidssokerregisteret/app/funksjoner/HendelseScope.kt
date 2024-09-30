package no.nav.paw.arbeidssokerregisteret.app.funksjoner

data class HendelseScope<A>(
    val key: A,
    /**Unik id for personen som hendelsen gjelder, generert av paw-kafka-key-generator*/
    val id: Long,
    val partition: Int,
    val offset: Long
)

interface HasRecordScope<A> {
    val hendelseScope: HendelseScope<A>?
}
