package no.nav.paw.arbeidssoekerregisteret.utils

import no.nav.paw.arbeidssoekerregisteret.model.VarselHendelse
import no.nav.paw.serialization.kafka.JacksonSerde

class VarselHendelseJsonSerde : JacksonSerde<VarselHendelse>(VarselHendelse::class)