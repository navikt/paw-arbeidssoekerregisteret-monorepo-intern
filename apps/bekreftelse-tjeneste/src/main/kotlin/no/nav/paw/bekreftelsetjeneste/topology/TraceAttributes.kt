package no.nav.paw.bekreftelsetjeneste.topology

import io.opentelemetry.api.common.AttributeKey

const val meldingIgnorert = "bekreftelse.ignorert"

val bekreftelseloesingKey = AttributeKey.stringKey("bekreftelsesloesning")
val periodeFunnetKey = AttributeKey.booleanKey("periode_funnet")
val harAnsvar = AttributeKey.booleanKey("har_ansvar")