package no.nav.paw.bekreftelsetjeneste.config

import java.nio.file.Paths

object StaticConfigValues {
    val arenaSyncFile = Paths.get("/var/run/secrets/paw-arbeidssoekere-bekreftelse-uke-sync/v1.csv")
}