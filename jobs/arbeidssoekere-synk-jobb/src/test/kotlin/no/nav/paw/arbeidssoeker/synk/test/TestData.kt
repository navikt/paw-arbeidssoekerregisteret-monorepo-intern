package no.nav.paw.arbeidssoeker.synk.test

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.common.runBlocking
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import no.nav.paw.arbeidssoeker.synk.model.AarsakTilAvvisning
import no.nav.paw.arbeidssoeker.synk.model.AvvisningRegel
import no.nav.paw.arbeidssoeker.synk.model.OpprettPeriodeErrorResponse
import no.nav.paw.serialization.jackson.buildObjectMapper
import kotlin.coroutines.coroutineContext

fun OpprettPeriodeErrorResponse.asJson(): String = buildObjectMapper.writeValueAsString(this)

object ErrorResponse {

    val ikkeTilgang
        get(): Pair<HttpStatusCode, String> =
            HttpStatusCode.Forbidden to OpprettPeriodeErrorResponse(
                melding = "Å nei du",
                feilKode = "IKKE_TILGANG",
                aarsakTilAvvisning = AarsakTilAvvisning(
                    detaljer = listOf("ANSATT_IKKE_TILGANG"),
                    regler = listOf(
                        AvvisningRegel(
                            id = "IKKE_TILGANG",
                            beskrivelse = "Ansatt har ikke tilgang"
                        )
                    )
                )
            ).asJson()

    val avvist
        get(): Pair<HttpStatusCode, String> =
            HttpStatusCode.BadRequest to OpprettPeriodeErrorResponse(
                melding = "Ikke prøv deg",
                feilKode = "AVVIST",
                aarsakTilAvvisning = AarsakTilAvvisning(
                    detaljer = listOf("UGYLDIG_FEILRETTING"),
                    regler = listOf(
                        AvvisningRegel(
                            id = "ENDRE_FOR_ANNEN_BRUKER",
                            beskrivelse = "Ugyldig feilretting"
                        )
                    )
                )
            ).asJson()

    val ukjentFeil
        get(): Pair<HttpStatusCode, String> =
            HttpStatusCode.InternalServerError to OpprettPeriodeErrorResponse(
                melding = "Dette gikk skikkelig dårlig",
                feilKode = "UKJENT_FEIL"
            ).asJson()
}

inline fun <reified T> OutgoingContent.readValue(): T {
    val byteReadChannel: ByteReadChannel = when (val body = this) {
        is OutgoingContent.ByteArrayContent -> ByteReadChannel(body.bytes())
        is OutgoingContent.WriteChannelContent -> body.writeBytes()
        is OutgoingContent.ReadChannelContent -> body.readFrom()
        is OutgoingContent.NoContent -> error("OutgoingContent is NoContent")
        is OutgoingContent.ProtocolUpgrade -> error("OutgoingContent is ProtocolUpgrade")
        is OutgoingContent.ContentWrapper -> error("OutgoingContent is ContentWrapper")
    }
    return buildObjectMapper.readValue<T>(byteReadChannel.asString())
}

fun ByteReadChannel.asString(): String = runBlocking {
    val sb = StringBuilder()
    while (!isClosedForRead) {
        sb.append(readUTF8Line() ?: "")
    }
    sb.toString()
}

@OptIn(DelicateCoroutinesApi::class)
fun OutgoingContent.WriteChannelContent.writeBytes(): ByteReadChannel = runBlocking {
    GlobalScope.writer(coroutineContext, autoFlush = true) { writeTo(channel) }.channel
}
