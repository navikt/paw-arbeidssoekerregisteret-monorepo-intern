package no.nav.paw.arbeidssokerregisteret.app

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.app.tilstand.TilstandSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata as InternMetadata
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.state.internals.InMemoryKeyValueBytesStoreSupplier
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import java.time.temporal.ChronoUnit
import java.util.*

const val SCHEMA_REGISTRY_SCOPE = "juni-registry"

val hendelseSerde = HendelseSerde()
val periodeSerde = opprettSerde<Periode>()
val opplysningerOmArbeidssoekerSerde = opprettSerde<OpplysningerOmArbeidssoeker>()
val tilstandSerde = TilstandSerde()
const val dbNavn = "tilstandsDb"

const val eventlogTopicNavn = "eventlogTopic"
const val periodeTopicNavn = "periodeTopic"
const val opplysningerOmArbeidssoekerTopicNavn = "opplysningerOmArbeidssoekerTopic"

fun verifiserPeriodeOppMotStartetOgStoppetHendelser(
    forventetKafkaKey: Long,
    startet: Startet,
    avsluttet: Avsluttet?,
    mottattRecord: KeyValue<Long, Periode>
) {
    mottattRecord.key shouldBe forventetKafkaKey
    mottattRecord.value.id shouldBe startet.hendelseId
    mottattRecord.value.identitetsnummer shouldBe startet.identitetsnummer
    mottattRecord.value.startet.tidspunkt shouldBe startet.metadata.tidspunkt.truncatedTo(ChronoUnit.MILLIS)
    mottattRecord.value.startet.aarsak shouldBe startet.metadata.aarsak
    mottattRecord.value.startet.utfoertAv.type.name shouldBe startet.metadata.utfoertAv.type.name
    mottattRecord.value.startet.utfoertAv.id shouldBe startet.metadata.utfoertAv.id
    if (avsluttet == null) {
        mottattRecord.value.avsluttet shouldBe null
    } else {
        val mottattApiMetadata = mottattRecord.value.avsluttet
        verifiserApiMetadataMotInternMetadata(avsluttet.metadata, mottattApiMetadata)
    }
}

fun verifiserApiMetadataMotInternMetadata(
    forventedeMetadataVerdier: InternMetadata,
    mottattApiMetadata: Metadata
) {
    mottattApiMetadata.shouldNotBeNull()
    mottattApiMetadata.tidspunkt shouldBe forventedeMetadataVerdier.tidspunkt.truncatedTo(ChronoUnit.MILLIS)
    mottattApiMetadata.aarsak shouldBe forventedeMetadataVerdier.aarsak
    mottattApiMetadata.utfoertAv.type.name shouldBe forventedeMetadataVerdier.utfoertAv.type.name
    mottattApiMetadata.utfoertAv.id shouldBe forventedeMetadataVerdier.utfoertAv.id
}

fun <T : SpecificRecord> opprettSerde(): Serde<T> {
    val schemaRegistryClient = MockSchemaRegistry.getClientForScope(SCHEMA_REGISTRY_SCOPE)
    val serde: Serde<T> = SpecificAvroSerde(schemaRegistryClient)
    serde.configure(
        mapOf(
            KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to "true",
            KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://$SCHEMA_REGISTRY_SCOPE"
        ),
        false
    )
    return serde
}

fun <T> opprettStreamsBuilder(dbNavn: String, tilstandSerde: Serde<T>): StreamsBuilder {
    val builder = StreamsBuilder()
    builder.addStateStore(
        KeyValueStoreBuilder(
            InMemoryKeyValueBytesStoreSupplier(dbNavn),
            Serdes.Long(),
            tilstandSerde,
            Time.SYSTEM
        )
    )
    return builder
}

val kafkaStreamProperties = Properties().apply {
    this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
    this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
    this[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.Long().javaClass
    this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = SpecificAvroSerde<SpecificRecord>().javaClass
    this[KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS] = "true"
    this[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://$SCHEMA_REGISTRY_SCOPE"
}

