package no.nav.paw.arbeidssokerregisteret.profilering

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheus.PrometheusMeterRegistry

fun PrometheusMeterRegistry.registerTopicVersionGauge(vararg topicInfos: TopicInfo) {
    topicInfos.forEach { topicInfo ->
        registerTopicVersionGauge(
            topicInfo.version,
            topicInfo.description,
            topicInfo.topic,
            topicInfo.messageType,
            topicInfo.topicOperation
        )
    }
}

fun topicInfo(
    topic: String,
    messageType: String,
    description: String,
    topicOperation: TopicOperation
): TopicInfo = TopicInfo(topic, messageType, description, topicOperation)

data class TopicInfo(
    val topic: String,
    val messageType: String,
    val description: String,
    val topicOperation: TopicOperation
) {
    val version: Int = topicNameToVersion(topic)
}

fun PrometheusMeterRegistry.registerTopicVersionGauge(
    version: Int,
    description: String,
    topic: String,
    messageType: String,
    topicOperation: TopicOperation
) {
    gauge(
        "paw_topic_version_gauge",
        Tags.of(
            Tag.of("topic", topic),
            Tag.of("messageType", messageType),
            Tag.of("description", description),
            Tag.of("operation", topicOperation.name)
        ),
        version
    ) { version.toDouble() }
}

enum class TopicOperation {
    READ,
    WRITE,
    READ_WRITE
}

fun topicNameToVersion(name: String): Int =
    name
        .split("-")
        .reversed()
        .firstOrNull()
        ?.replace("v", "")
        ?.let(String::toInt)
        ?: -1