package no.nav.paw.tilgangskontroll.poaotilgang

import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

data class PoaoConfig(
    val scope: String,
    val url: String
)

fun loadPoaoConfig(configFile: String = "poao_tilgang_cfg.toml"): PoaoConfig = loadNaisOrLocalConfiguration(configFile)
