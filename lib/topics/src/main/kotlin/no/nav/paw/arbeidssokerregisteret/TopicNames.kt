package no.nav.paw.arbeidssokerregisteret

import no.nav.paw.config.env.DevGcp
import no.nav.paw.config.env.Local
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

data class TopicNames(
    val periodeTopic: String,
    val opplysningerTopic: String,
    val profileringTopic: String,
    val bekreftelseTopic: String,
    val paavnegneavTopic: String,
    val egenvurderingTopic: String
)

fun TopicNames.asList(): List<String> = listOf(
    periodeTopic,
    opplysningerTopic,
    profileringTopic,
    bekreftelseTopic,
    paavnegneavTopic,
    egenvurderingTopic
)

fun standardTopicNames(naisEnv: RuntimeEnvironment = currentRuntimeEnvironment): TopicNames =
    when(naisEnv) {
        is DevGcp -> "dev_topic_names.toml"
        is ProdGcp -> "prod_topic_names.toml"
        Local -> "topic_names.toml"
    }.let(::loadNaisOrLocalConfiguration)
