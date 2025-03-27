package no.nav.paw.arbeidssoeker.synk.config

import no.nav.paw.arbeidssoeker.synk.model.Feiltype
import no.nav.paw.arbeidssoeker.synk.model.PeriodeTilstand
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment

const val JOB_CONFIG = "job_config.toml"

data class JobConfig(
    val jobEnabled: Boolean,
    val syncFilePath: String,
    val defaultVerdier: DefaultVerdier,
    val apiInngang: ApiInngangConfig,
    val runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment
)

data class DefaultVerdier(
    val periodeTilstand: PeriodeTilstand,
    val forhaandsgodkjentAvAnsatt: Boolean,
    val feilrettingFeiltype: Feiltype,
    val feilrettingMelding: String
)

data class ApiInngangConfig(
    val baseUrl: String,
    val scope: String,
)
