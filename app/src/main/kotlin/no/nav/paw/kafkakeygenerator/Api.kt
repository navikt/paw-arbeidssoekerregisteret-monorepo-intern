package no.nav.paw.kafkakeygenerator

class Api(
    private val kafkaKeys: KafkaKeys,
    private val aliasTjeneste: AliasTjeneste
) {
    fun hello(): String = "Hello, World!"

    fun hentEllerOpprett(identitet: String): Long? =
        sequenceOf(
            { kafkaKeys.hent(identitet) },
            { sjekkMotAliaser(identitet) },
            { kafkaKeys.opprett(identitet) },
            //'Opprett' returnerer null ved konflikt, så da skal hent finne noe
            { kafkaKeys.hent(identitet) }
        )
            .map { it.invoke() }
            .filterNotNull()
            .firstOrNull()

    private fun sjekkMotAliaser(identitet: String): Long? {
        val aliaser = aliasTjeneste.hentAlias(identitet)
        return kafkaKeys.hent(aliaser)
            .values
            .firstOrNull()
            ?.also { nøkkel ->
                kafkaKeys.lagre(identitet, nøkkel)
            }
    }
}