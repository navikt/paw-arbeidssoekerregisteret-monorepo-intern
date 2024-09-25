package no.nav.paw.bekreftelse.api

import no.nav.paw.bekreftelse.api.model.BekreftelseRequest
import no.nav.paw.bekreftelse.api.model.TilgjengeligBekreftelse
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import org.apache.kafka.streams.KeyQueryMetadata
import org.apache.kafka.streams.state.HostInfo
import java.time.Instant
import java.util.*

class TestDataGenerator {

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
    ) = KeyQueryMetadata(nyHostInfo(), emptySet(), 1)

    fun nyHostInfo() = HostInfo("localhost", 9092)
}