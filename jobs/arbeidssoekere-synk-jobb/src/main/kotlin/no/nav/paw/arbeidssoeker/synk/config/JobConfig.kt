package no.nav.paw.arbeidssoeker.synk.config

import no.nav.paw.arbeidssoeker.synk.model.PeriodeTilstand
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment

const val JOB_CONFIG = "job_config.toml"

data class JobConfig(
    val syncFilePath: String,
    val defaultVerdier: DefaultVerdier,
    val apiInngang: ApiInngangConfig,
    val runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
)

data class DefaultVerdier(
    val periodeTilstand: PeriodeTilstand,
    val forhaandsgodkjentAvAnsatt: Boolean
)

data class ApiInngangConfig(
    val baseUrl: String,
    val scope: String,
)
