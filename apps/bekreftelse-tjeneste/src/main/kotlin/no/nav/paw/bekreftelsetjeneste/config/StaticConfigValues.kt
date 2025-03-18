package no.nav.paw.bekreftelsetjeneste.config

import java.nio.file.Paths

object StaticConfigValues {
    val syncMappe = Paths.get("/var/run/secrets/")
    val starterMed = "paw-arbeidssoekere-bekreftelse-uke-sync"
    val filenavn = "v1.csv"
}