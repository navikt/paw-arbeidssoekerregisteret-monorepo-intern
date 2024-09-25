package no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse

import no.nav.paw.bekreftelse.internehendelser.BekreftelseMeldingMottatt
import java.time.Instant
import java.util.*

fun bekreftelseMottatt(
    bekreftelseId: UUID = UUID.randomUUID(),
    periodeId: UUID = UUID.randomUUID(),
    hendelseId: UUID = UUID.randomUUID(),
    arbeidssoekerId: Long = 1L,
    hendelseTidspunkt: Instant = Instant.now()
) = BekreftelseMeldingMottatt(
    bekreftelseId = bekreftelseId,
    periodeId = periodeId,
    hendelseId = hendelseId,
    arbeidssoekerId = arbeidssoekerId,
    hendelseTidspunkt = hendelseTidspunkt
)