package no.nav.paw.arbeidssokerregisteret.app.metrics


private const val PREFIX = "paw_arbeidssokerregisteret"
object Names {
    const val MESSAGE = "${PREFIX}_message"
    const val LATENCY = "${PREFIX}_latency"
}

object Labels {
    const val PARTITION = "partition"
    const val MESSAGE_TYPE = "type"
    const val TOPIC = "topic"
    const val ACTION = "action"
    const val DIRECTION = "direction"
}

object Directions {
    const val IN = "in"
    const val OUT = "out"
}

object Actions {
    val START = "start_periode"
    val STOP = "avslutt_periode"
    val INFO_RECEIVED = "opplysninger_mottatt"
    val UNKNOWN = "ukjent"
}