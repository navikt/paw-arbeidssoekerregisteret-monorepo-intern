package no.nav.paw.bekreftelse.api

import io.mockk.mockk
import kotlinx.coroutines.Deferred
import no.nav.paw.bekreftelse.api.model.BekreftelseRow
import no.nav.paw.bekreftelse.api.model.MottaBekreftelseRequest
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import org.apache.kafka.clients.producer.RecordMetadata
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.Future

class TestDataGenerator {

    val fnr1 = "01017012345"
    val fnr2 = "02017012345"
    val fnr3 = "03017012345"
    val arbeidssoekerId1 = 10001L
    val arbeidssoekerId2 = 10002L
    val arbeidssoekerId3 = 10003L
    val kafkaKey1 = -10001L
    val kafkaKey2 = -10002L
    val kafkaKey3 = -10003L
    val periodeId1 = UUID.fromString("4c0cb50a-3b4a-45df-b5b6-2cb45f04d19b")
    val periodeId2 = UUID.fromString("0fc3de47-a6cd-4ad5-8433-53235738200d")
    val bekreftelseId1 = UUID.fromString("0cd73e66-e5a2-4dae-88de-2dd89a910a19")
    val bekreftelseId2 = UUID.fromString("7b769364-4d48-40f8-ac64-4489bb8080dd")
    val bekreftelseId3 = UUID.fromString("b6e3b543-da44-4524-860f-9474bd6d505e")
    val hendelseId1 = UUID.fromString("d69695e0-4249-4756-b0ef-02979ac66fea")
    val hendelseId2 = UUID.fromString("9830a768-553c-4e11-b1f8-165b4e499be7")

    fun nyBekreftelseRow(
        version: Int = 1,
        partition: Int = 1,
        offset: Long = 1,
        recordKey: Long = kafkaKey1,
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
        return bekreftelseRow.mapIndexed { index, value ->
            nyBekreftelseRow(
                offset = value.first,
                arbeidssoekerId = arbeidssoekerId,
                periodeId = periodeId,
                bekreftelseId = value.second
            )
        }
    }

    fun nyTilgjengeligBekreftelse(
        periodeId: UUID = periodeId1,
        bekreftelseId: UUID = bekreftelseId1,
        gjelderFra: Instant = Instant.now(),
        gjelderTil: Instant = Instant.now().plus(Duration.ofDays(14)),
    ) = TilgjengeligBekreftelse(
        periodeId = periodeId,
        bekreftelseId = bekreftelseId,
        gjelderFra = gjelderFra,
        gjelderTil = gjelderTil
    )

    fun nyBekreftelseTilgjengelig(
        hendelseId: UUID = hendelseId1,
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

    fun nyProducerFuture() = mockk<Future<RecordMetadata>>()
    fun nyProducerDeferred() = mockk<Deferred<RecordMetadata>>()

    fun nyBekreftelseMeldingMottatt(
        hendelseId: UUID = hendelseId1,
        periodeId: UUID = periodeId1,
        arbeidssoekerId: Long = arbeidssoekerId1,
        hendelseTidspunkt: Instant = Instant.now(),
        bekreftelseId: UUID = bekreftelseId1
    ) = BekreftelseMeldingMottatt(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidssoekerId,
        hendelseTidspunkt = hendelseTidspunkt,
        bekreftelseId = bekreftelseId,
    )
}