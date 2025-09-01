package no.nav.paw.kafkakeygenerator.test

import org.apache.kafka.clients.producer.internals.BuiltInPartitioner
import org.apache.kafka.common.serialization.Serdes

const val NUMBER_OF_PARTITIONS = 6

fun main() {
    val recordKey: Long = 123
    val partition = BuiltInPartitioner.partitionForKey(Serdes.Long().serializer().serialize("topic", recordKey), NUMBER_OF_PARTITIONS)
    println("Record key $recordKey is assigned to partition $partition")
}