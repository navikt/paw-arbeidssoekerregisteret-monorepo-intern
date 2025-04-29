package no.nav.paw.arbeidssoekerregisteret.backup.config

import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig

data class AzureConfig(
    val name: String,
    val discoveryUrl: String,
    val tokenEndpointUrl: String,
    val clientId: String
)

val AzureConfig.m2mCfg: AzureM2MConfig get() = AzureM2MConfig(tokenEndpointUrl, clientId)