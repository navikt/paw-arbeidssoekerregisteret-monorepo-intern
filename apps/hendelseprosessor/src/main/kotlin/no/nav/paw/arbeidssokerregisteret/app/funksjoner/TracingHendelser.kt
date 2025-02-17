package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributesBuilder
import no.nav.paw.arbeidssokerregisteret.app.StreamHendelse

const val hendelseIgnorert = "arbeidssoekerregisteret_hendelse_ignorert"
const val hendelseAkseptert = "arbeidssoekerregisteret_hendelse_akseptert"

val hendelseTypeKey = AttributeKey.stringKey("arbeidssoekerregisteret_hendelse_type")
val tilstandKey = AttributeKey.stringKey("paw.arbeidssoekerregisteret.tilstand")

fun hendelseType(hendelse: StreamHendelse): Pair<AttributeKey<String>, String> =
     hendelseTypeKey to hendelse.hendelseType.replace(".", "_")

fun aarsak(aarsak: String): Pair<AttributeKey<String>, String> =
    AttributeKey.stringKey("aarsak") to aarsak

fun <A: Any> AttributesBuilder.add(kv: Pair<AttributeKey<A>, A>) : AttributesBuilder = this.put(kv.first, kv.second)