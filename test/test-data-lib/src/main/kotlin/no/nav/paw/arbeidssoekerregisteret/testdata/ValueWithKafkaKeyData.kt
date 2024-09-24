package no.nav.paw.arbeidssoekerregisteret.testdata

data class ValueWithKafkaKeyData<V>(
    val id : Long,
    val key: Long,
    val value: V
)