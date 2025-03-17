package no.nav.paw.bekreftelsetjeneste.topology

import io.opentelemetry.api.common.AttributeKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

//hendelser
const val intern = "intern"
const val okEvent = "ok"
const val errorEvent = "error"


//felles
val bekreftelseloesingKey = AttributeKey.stringKey("bekreftelsesloesning")
val periodeFunnetKey = AttributeKey.booleanKey("periode_funnet")
val harAnsvarKey = AttributeKey.booleanKey("har_ansvar")
val feilMeldingKey = AttributeKey.stringKey("feilmelding")
val domainKey = AttributeKey.stringKey("domain")
val actionKey = AttributeKey.stringKey("action")
val fraOgMedDagKey = AttributeKey.stringKey("fra_og_meg_dag")
val tilDagKey = AttributeKey.stringKey("til_dag")

private val fraTilKeyValueformatter = DateTimeFormatter.ofPattern("yyyyMMdd")
val norskTid = ZoneId.of("Europe/Oslo")
fun Instant.tilFraTilAttributeKeyValue(): String = LocalDate.ofInstant(this, norskTid).format(fraTilKeyValueformatter)

//Bekreftelse
val tilstandKey = AttributeKey.stringKey("tilstand")
val initielBekreftelseKey = AttributeKey.booleanKey("initiel_bekreftelse")
val nyBekreftelseStatusKey = AttributeKey.stringKey("ny_bekreftelse_status")
val gjeldeneBekreftelseStatusKey = AttributeKey.stringKey("gjeldene_bekreftelse_status")
const val bekreftelseLevertAction = "bekreftelse_levert"
const val bekreftelseOpprettetAction = "bekreftelse_opprettet"
const val bekreftelseSattStatusAction = "bekreftelse_satt_status"


//PaaVegneAv
const val paaVegneAvStartet ="paa_vegne_av_startet"
const val paaVegneAvStoppet ="paa_vegne_av_stoppet"
val registerFristUtloept = AttributeKey.booleanKey("register_frist_utloept")


fun String.snakeCase(): String = this.replace(Regex("([a-z])([A-Z]+)"), "$1_$2").lowercase()
