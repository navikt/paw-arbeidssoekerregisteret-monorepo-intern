package no.nav.paw.arbeidssokerregisteret.app.metrics

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun PrometheusMeterRegistry.eventReceived(topic: String, messageType: String, action: String) {
    counter(
        Names.MESSAGE,
        Labels.DIRECTION, Directions.IN,
        Labels.TOPIC, topic,
        Labels.MESSAGE_TYPE, messageType,
        Labels.ACTION, action
    ).increment()
}

fun PrometheusMeterRegistry.stateSent(topic: String, messageType: String, action: String) {
    counter(
        Names.MESSAGE,
        Labels.DIRECTION, Directions.OUT,
        Labels.TOPIC, topic,
        Labels.MESSAGE_TYPE, messageType,
        Labels.ACTION, action
    ).increment()
}