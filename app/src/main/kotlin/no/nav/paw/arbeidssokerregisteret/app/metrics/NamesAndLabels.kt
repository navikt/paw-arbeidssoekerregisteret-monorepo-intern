package no.nav.paw.arbeidssokerregisteret.app.metrics


private const val PREFIX = "paw_arbeidssokerregisteret"
object Names {
    const val MESSAGE = "${PREFIX}_message"
    const val LATENCY = "${PREFIX}_latency"
    const val AVRO_SCHEMA_AGE = "${PREFIX}_avro_schema_age"
    const val AVRO_SCHEMA_BUILD_TIME = "${PREFIX}_avro_schema_build_time"
    const val AVRO_MAJOR_VERSION = "${PREFIX}_avro_major_version"
    const val STREAM_STATE = "${PREFIX}_stream_state"
    const val ARBEIDSSOEKER_ANTALL = "${PREFIX}_arbeidssoker_antall_v2"
    const val ARBEIDSSOEKER_JOBB_SITUASJON = "${PREFIX}_arbeidssoker_jobb_situasjon_v2"
    const val ANTALL_TILSTANDER = "${PREFIX}_arbeidssoker_antall_tilstander_v2"
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
    val REJECTED = "avvist"
    val START = "start_periode"
    val STOP = "avslutt_periode"
    val INFO_RECEIVED = "opplysninger_mottatt"
    val UNKNOWN = "ukjent"
}