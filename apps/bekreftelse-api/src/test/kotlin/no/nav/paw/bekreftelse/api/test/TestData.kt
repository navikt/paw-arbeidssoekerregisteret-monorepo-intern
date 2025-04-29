package no.nav.paw.bekreftelse.api.test

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import no.nav.paw.bekreftelse.api.model.BekreftelseRow
import no.nav.paw.bekreftelse.api.models.MottaBekreftelseRequest
import no.nav.paw.bekreftelse.api.models.TilgjengeligeBekreftelserRequest
import no.nav.paw.bekreftelse.api.repository.BekreftelseRepository
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.*

inline fun <reified T> HttpRequestBuilder.setJsonBody(body: T) {
    headers {
        append(HttpHeaders.ContentType, ContentType.Application.Json)
    }
    setBody(body)
}

fun BekreftelseRepository.opprettBekreftelser(bekreftelseRows: Iterable<BekreftelseRow>) {
    transaction {
        bekreftelseRows.forEach(::insert)
    }
}

object TestData {

    val fnr1 = "01017012345"
    val fnr2 = "02017012345"
    val fnr3 = "03017012345"
    val fnr4 = "04017012345"
    val fnr5 = "05017012345"
    val arbeidssoekerId1 = 10001L
    val arbeidssoekerId2 = 10002L
    val arbeidssoekerId3 = 10003L
    val arbeidssoekerId4 = 10004L
    val arbeidssoekerId5 = 10005L
    val arbeidssoekerId6 = 10006L
    val key1 = -10001L
    val key2 = -10002L
    val key3 = -10003L
    val key4 = -10004L
    val key5 = -10005L
    val key6 = -10006L
    val offset1 = 1L
    val offset2 = 2L
    val offset3 = 3L
    val offset4 = 4L
    val offset5 = 5L
    val offset6 = 6L
    val offset7 = 7L
    val offset8 = 8L
    val periodeId1 = UUID.fromString("4c0cb50a-3b4a-45df-b5b6-2cb45f04d19b")
    val periodeId2 = UUID.fromString("0fc3de47-a6cd-4ad5-8433-53235738200d")
    val periodeId3 = UUID.fromString("12cf8147-a76d-4b62-85d2-4792fea08995")
    val periodeId4 = UUID.fromString("f6f2f98a-2f2b-401d-b837-6ad26e45d4bf")
    val periodeId5 = UUID.fromString("f6384bc5-a0ec-4bdc-9262-f6ebf952269f")
    val bekreftelseId1 = UUID.fromString("0cd73e66-e5a2-4dae-88de-2dd89a910a19")
    val bekreftelseId2 = UUID.fromString("7b769364-4d48-40f8-ac64-4489bb8080dd")
    val bekreftelseId3 = UUID.fromString("b6e3b543-da44-4524-860f-9474bd6d505e")
    val bekreftelseId4 = UUID.fromString("a59581e6-c9be-4aec-b9f4-c635f1826c71")
    val bekreftelseId5 = UUID.fromString("de94b7ab-360f-4e5f-9ad1-3dc572b3e6a5")
    val bekreftelseId6 = UUID.fromString("095779b3-64c5-4609-afcd-7392bd33b2d0")
    val bekreftelseId7 = UUID.fromString("92f92510-182b-4681-bd6f-9984b2c329a5")
    val bekreftelseId8 = UUID.fromString("a1342750-73e9-43e6-93b1-739e99ee79cf")
    val navIdent1 = "NAV1001"
    val navIdent2 = "NAV1002"
    val navIdent3 = "NAV1003"

    fun nyBekreftelseRow(
        version: Int = 1,
        partition: Int = 1,
        offset: Long = 1,
        recordKey: Long = key1,
        arbeidssoekerId: Long = arbeidssoekerId1,
        periodeId: UUID = periodeId1,
        bekreftelseId: UUID = bekreftelseId1,
        data: BekreftelseTilgjengelig = nyBekreftelseTilgjengelig(
            arbeidssoekerId = arbeidssoekerId,
            periodeId = periodeId,
            bekreftelseId = bekreftelseId,
        )
    ) = BekreftelseRow(
        version = version,
        partition = partition,
        offset = offset,
        recordKey = recordKey,
        arbeidssoekerId = arbeidssoekerId,
        periodeId = periodeId,
        bekreftelseId = bekreftelseId,
        data = data
    )

    fun nyBekreftelseRows(
        arbeidssoekerId: Long = arbeidssoekerId1,
        periodeId: UUID = periodeId1,
        bekreftelseRow: List<Pair<Long, UUID>>
    ): List<BekreftelseRow> {
        return bekreftelseRow.map {
            nyBekreftelseRow(
                offset = it.first,
                arbeidssoekerId = arbeidssoekerId,
                periodeId = periodeId,
                bekreftelseId = it.second
            )
        }
    }

    fun nyBekreftelseTilgjengelig(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = periodeId1,
        arbeidssoekerId: Long = arbeidssoekerId1,
        bekreftelseId: UUID = bekreftelseId1,
        gjelderFra: Instant = Instant.now(),
        gjelderTil: Instant = Instant.now().plus(Duration.ofDays(14)),
        hendelseTidspunkt: Instant = Instant.now()
    ) = BekreftelseTilgjengelig(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt,
        bekreftelseId = bekreftelseId,
        gjelderFra = gjelderFra,
        gjelderTil = gjelderTil
    )

    fun nyTilgjengeligeBekreftelserRequest(
        identitetsnummer: String? = null
    ) = TilgjengeligeBekreftelserRequest(
        identitetsnummer = identitetsnummer
    )

    fun nyBekreftelseRequest(
        identitetsnummer: String? = null,
        bekreftelseId: UUID = bekreftelseId1,
        harJobbetIDennePerioden: Boolean = false,
        vilFortsetteSomArbeidssoeker: Boolean = true
    ) = MottaBekreftelseRequest(
        identitetsnummer = identitetsnummer,
        bekreftelseId = bekreftelseId,
        harJobbetIDennePerioden = harJobbetIDennePerioden,
        vilFortsetteSomArbeidssoeker = vilFortsetteSomArbeidssoeker
    )
}