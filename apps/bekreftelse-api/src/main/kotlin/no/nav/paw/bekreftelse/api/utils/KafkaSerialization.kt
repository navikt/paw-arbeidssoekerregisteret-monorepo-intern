package no.nav.paw.bekreftelse.api.utils

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse

class BekreftelseAvroSerializer : SpecificAvroSerializer<Bekreftelse>()
