package no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse

import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import java.time.Instant
import java.util.*

fun periodeAvsluttet(
    hendelseId: UUID = UUID.randomUUID(),
    periodeId: UUID = UUID.randomUUID(),
    arbeidssøkerId: Long = 1L,
    hendelseTidspunkt: Instant = Instant.now()
) = PeriodeAvsluttet(
    hendelseId = hendelseId,
    periodeId = periodeId,
    arbeidssoekerId = arbeidssøkerId,
    hendelseTidspunkt = hendelseTidspunkt
)