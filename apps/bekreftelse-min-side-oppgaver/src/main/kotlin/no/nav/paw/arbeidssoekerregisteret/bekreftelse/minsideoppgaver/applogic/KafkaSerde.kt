package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.VarselHendelse
import no.nav.paw.serialization.kafka.JacksonSerde

class VarselHendelseJsonSerde : JacksonSerde<VarselHendelse>(VarselHendelse::class)