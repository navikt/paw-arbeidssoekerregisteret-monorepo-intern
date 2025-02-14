package no.nav.paw.arbeidssoeker.synk.config

import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment

const val JOB_CONFIG = "job_config.toml"

data class JobConfig(
    val syncFilePath: String,
    val apiInngangBaseUrl: String,
    val apiInngangScope: String,
    val runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
)
