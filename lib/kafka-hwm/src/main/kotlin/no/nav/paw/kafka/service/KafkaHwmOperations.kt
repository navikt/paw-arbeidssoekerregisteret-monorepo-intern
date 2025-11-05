package no.nav.paw.kafka.service

import no.nav.paw.kafka.model.Hwm
import java.time.Instant

interface KafkaHwmOperations {
    fun initHwm(topic: String, partitionCount: Int): Int
    fun getHwm(topic: String, partition: Int): Hwm
    fun updateHwm(topic: String, partition: Int, offset: Long, timestamp: Instant): Int
}