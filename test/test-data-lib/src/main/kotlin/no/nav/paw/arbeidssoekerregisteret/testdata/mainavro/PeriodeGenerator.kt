package no.nav.paw.arbeidssoekerregisteret.testdata.mainavro

import no.nav.paw.arbeidssoekerregisteret.testdata.KafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.ValueWithKafkaKeyData
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.util.*


fun KafkaKeyContext.periode(
    periodeId: UUID = UUID.randomUUID(),
    identitetsnummer: String = "12345678901",
    startetMetadata: Metadata = metadata(),
    avsluttetMetadata: Metadata? = null
): ValueWithKafkaKeyData<Periode> = Periode.newBuilder()
    .setId(periodeId)
    .setIdentitetsnummer(identitetsnummer)
    .setStartet(startetMetadata)
    .setAvsluttet(avsluttetMetadata)
    .build().let { periode ->
        val kv = getKafkaKey(periode.identitetsnummer)
        ValueWithKafkaKeyData(kv.id, kv.key, periode)
    }
