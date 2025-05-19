package no.nav.paw.bqadapter

import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration


val appConfig: AppConfig get() = loadNaisOrLocalConfiguration("app.toml")

data class AppConfig(
    val bigqueryProject: String
)