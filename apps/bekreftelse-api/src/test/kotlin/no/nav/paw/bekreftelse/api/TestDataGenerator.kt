package no.nav.paw.bekreftelse.api

import no.nav.paw.bekreftelse.api.model.BekreftelseRequest
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import org.apache.kafka.streams.KeyQueryMetadata
import org.apache.kafka.streams.state.HostInfo
import java.time.Instant
import java.util.*

class TestDataGenerator {

    val fnr1 = "01017012345"
    val fnr2 = "02017012345"
    val arbeidsoekerId1 = 10001L
    val arbeidsoekerId2 = 10002L
    val kafkaKey1 = -10001L
    val kafkaKey2 = -10002L

    fun nyTilgjengeligBekreftelse(
        periodeId: UUID = UUID.randomUUID(),
        bekreftelseId: UUID = UUID.randomUUID(),
        gjelderFra: Instant = Instant.now(),
        gjelderTil: Instant = Instant.now()
    ) = TilgjengeligBekreftelse(periodeId, bekreftelseId, gjelderFra, gjelderTil)

    fun nyBekreftelseTilgjengelig(
        hendelseId: UUID = UUID.randomUUID(),
        periodeId: UUID = UUID.randomUUID(),
        arbeidsoekerId: Long = Random().nextLong(),
        bekreftelseId: UUID = UUID.randomUUID(),
        gjelderFra: Instant = Instant.now(),
        gjelderTil: Instant = Instant.now(),
        hendelseTidspunkt: Instant = Instant.now()
    ) = BekreftelseTilgjengelig(
        hendelseId = hendelseId,
        periodeId = periodeId,
        arbeidssoekerId = arbeidsoekerId,
        hendelseTidspunkt = hendelseTidspunkt,
        bekreftelseId = bekreftelseId,
        gjelderFra = gjelderFra,
        gjelderTil = gjelderTil
    )

    fun nyBekreftelseRequest(
        identitetsnummer: String? = null,
        bekreftelseId: UUID = UUID.randomUUID(),
        harJobbetIDennePerioden: Boolean = false,
        vilFortsetteSomArbeidssoeker: Boolean = true
    ) = BekreftelseRequest(identitetsnummer, bekreftelseId, harJobbetIDennePerioden, vilFortsetteSomArbeidssoeker)

    fun nyKeyQueryMetadata(
        host: String = "10.0.0.100"
    ) = KeyQueryMetadata(HostInfo(host, 8080), emptySet(), 1)
}