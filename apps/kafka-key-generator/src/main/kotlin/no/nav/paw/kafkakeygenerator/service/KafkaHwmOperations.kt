package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.kafkakeygenerator.model.Hwm
import java.time.Instant

interface KafkaHwmOperations {
    fun initHwm(topic: String, partitionCount: Int): Int
    fun getHwm(topic: String, partition: Int): Hwm
    fun updateHwm(topic: String, partition: Int, offset: Long, timestamp: Instant): Int
}