package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import no.nav.paw.kafkakeygenerator.auth.AzureM2MConfig
import no.nav.paw.kafkakeygenerator.auth.azureAdM2MTokenClient
import no.nav.paw.kafkakeygenerator.auth.currentNaisEnv
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.createKafkaKeyGeneratorClient

fun initClients(azureConfig: AzureM2MConfig): Pair<KafkaKeysClient, OppslagApiClient> {
    val azureTokenClient = azureAdM2MTokenClient(currentNaisEnv, azureConfig)
    val kafkaKeysClient = createKafkaKeyGeneratorClient(azureTokenClient)
    return kafkaKeysClient to OppslagApiClient()
}