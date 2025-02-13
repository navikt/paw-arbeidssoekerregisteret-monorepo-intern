package no.nav.paw.bekreftelsetjeneste.topology

import io.opentelemetry.api.common.AttributeKey

//hendelser
const val okEvent = "ok"
const val errorEvent = "error"

//felles
val bekreftelseloesingKey = AttributeKey.stringKey("bekreftelsesloesning")
val periodeFunnetKey = AttributeKey.booleanKey("periode_funnet")
val harAnsvarKey = AttributeKey.booleanKey("har_ansvar")
val feilMeldingKey = AttributeKey.stringKey("feilmelding")
val domainKey = AttributeKey.stringKey("domain")
val actionKey = AttributeKey.stringKey("action")

//Bekreftelse
val tilstandKey = AttributeKey.stringKey("tilstand")
const val bekreftelseLevertAction = "bekreftelse_levert"

//PaaVegneAv
const val paaVegneAvStartet ="paa_vegne_av_startet"
const val paaVegneAvStoppet ="paa_vegne_av_stoppet"
