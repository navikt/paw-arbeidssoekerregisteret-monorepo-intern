package no.nav.paw.arbeidssokerregisteret.app

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import org.apache.kafka.streams.state.internals.RocksDbKeyValueBytesStoreSupplier

fun <T> konfigurerKafkaStrøm(
    hendelseLog: String,
    tilstandSerde: Serde<PeriodeTilstandV1>,
    hendelseSerde: Serde<T>,
    dbNavn: String,
): Pair<StreamsBuilder, KStream<String, T>> {
    val builder = StreamsBuilder().addStateStore(
        KeyValueStoreBuilder(
            RocksDbKeyValueBytesStoreSupplier(dbNavn, false),
            Serdes.String(),
            tilstandSerde,
            Time.SYSTEM
        )
    )
    val stream  = builder.stream(
        hendelseLog, Consumed.with(
            Serdes.String(),
            hendelseSerde
        )
    )
    return builder to stream
}
