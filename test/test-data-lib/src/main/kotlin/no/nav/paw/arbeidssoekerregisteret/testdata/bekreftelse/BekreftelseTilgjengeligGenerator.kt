package no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse

import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import java.time.Duration
import java.time.Instant
import java.util.*


fun bekreftelseTilgjengelig(
    bekreftelseId: UUID = UUID.randomUUID(),
    periodeId: UUID = UUID.randomUUID(),
    hendelseId: UUID = UUID.randomUUID(),
    arbeidssoekerId: Long = 1L,
    gjelderFra: Instant = Instant.now(),
    gjelderTil: Instant = gjelderFra + Duration.ofDays(14),
    hendelseTidspunkt: Instant = Instant.now()
) = BekreftelseTilgjengelig(
    bekreftelseId = bekreftelseId,
    periodeId = periodeId,
    hendelseId = hendelseId,
    arbeidssoekerId = arbeidssoekerId,
    gjelderFra = gjelderFra,
    gjelderTil = gjelderTil,
    hendelseTidspunkt = hendelseTidspunkt
)