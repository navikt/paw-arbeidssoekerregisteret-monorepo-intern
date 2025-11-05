package no.nav.paw.kafkakeygenerator.model.dto

import no.nav.paw.kafkakeygenerator.model.dao.HwmRow

data class Hwm(
    val version: Int,
    val topic: String,
    val partition: Int,
    val offset: Long
)

fun HwmRow.asHwm(): Hwm = Hwm(
    version = version,
    topic = topic,
    partition = partition,
    offset = offset
)
