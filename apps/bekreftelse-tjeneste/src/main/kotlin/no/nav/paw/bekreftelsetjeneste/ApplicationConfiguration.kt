package no.nav.paw.bekreftelsetjeneste

import java.time.Duration

data class ApplicationConfiguration(
    val periodeTopic: String,
    val ansvarsTopic: String,
    val bekreftelseTopic: String,
    val bekreftelseHendelseloggTopic: String,
    val stateStoreName: String,
    val punctuateInterval: Duration
) {
    val pawNamespace = "paw"
}
