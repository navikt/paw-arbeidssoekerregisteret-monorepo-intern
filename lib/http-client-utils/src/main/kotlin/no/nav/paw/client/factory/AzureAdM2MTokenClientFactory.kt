package no.nav.paw.client.factory

import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import no.nav.common.token_client.builder.AzureAdTokenClientBuilder
import no.nav.common.token_client.cache.CaffeineTokenCache
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.paw.client.config.AzureAdM2MConfig
import no.nav.paw.config.env.Local
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

fun createAzureAdM2MTokenClient(
    runtimeEnvironment: RuntimeEnvironment = currentRuntimeEnvironment,
    azureProviderConfig: AzureAdM2MConfig
): AzureAdMachineToMachineTokenClient =
    when (runtimeEnvironment) {
        is Local -> AzureAdTokenClientBuilder.builder()
            .withClientId(azureProviderConfig.clientId)
            .withPrivateJwk(createMockRSAKey("azure"))
            .withTokenEndpointUrl(azureProviderConfig.tokenEndpointUrl)
            .buildMachineToMachineTokenClient()

        else -> AzureAdTokenClientBuilder.builder()
            .withNaisDefaults()
            .withCache(CaffeineTokenCache())
            .buildMachineToMachineTokenClient()
    }

private fun createMockRSAKey(keyID: String): String? = KeyPairGenerator
    .getInstance("RSA").let {
        it.initialize(2048)
        it.generateKeyPair()
    }.let {
        RSAKey.Builder(it.public as RSAPublicKey)
            .privateKey(it.private as RSAPrivateKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(keyID)
            .build()
            .toJSONString()
    }