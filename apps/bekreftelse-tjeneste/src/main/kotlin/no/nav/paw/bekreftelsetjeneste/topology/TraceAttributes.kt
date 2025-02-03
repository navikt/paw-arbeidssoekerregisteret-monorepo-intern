package no.nav.paw.bekreftelsetjeneste.topology

import io.opentelemetry.api.common.AttributeKey

//felles
val bekreftelseloesingKey = AttributeKey.stringKey("bekreftelsesloesning")
val periodeFunnetKey = AttributeKey.booleanKey("periode_funnet")
val harAnsvarKey = AttributeKey.booleanKey("har_ansvar")
val feilMeldingKey = AttributeKey.stringKey("feilmelding")

//Bekreftelse
const val bekreftelseMottattOK = "bekreftelse.melding.ok"
const val bekreftelseMottattFeil = "bekreftelse.melding.feil"
val tilstandKey = AttributeKey.stringKey("tilstand")

//PaaVegneAv
const val paaVegneAvMottattOk = "bekreftelse.pa_vegna_av.ok"
const val paaVegneAvMottattFeil = "bekreftelse.pa_vegna_av.feil"
val forespoerselTypeKey = AttributeKey.stringKey("forespoersel_type")

