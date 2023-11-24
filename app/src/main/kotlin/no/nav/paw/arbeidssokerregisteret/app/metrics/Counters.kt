package no.nav.paw.arbeidssokerregisteret.app.metrics

import io.micrometer.prometheus.PrometheusMeterRegistry

context(PrometheusMeterRegistry)
fun eventReceived(topic: String, messageType: String, action: String) {
    counter(
        Names.MESSAGE,
        Labels.DIRECTION, Directions.IN,
        Labels.TOPIC, topic,
        Labels.MESSAGE_TYPE, messageType,
        Labels.ACTION, action
    ).increment()
}

context(PrometheusMeterRegistry)
fun stateSent(topic: String, action: String) {
    counter(
        Names.MESSAGE,
        Labels.DIRECTION, Directions.OUT,
        Labels.TOPIC, topic,
        Labels.ACTION, action
    ).increment()
}