package no.nav.paw.arbeidssoekerregisteret.model

import java.util.*

data class AvsluttOppgave(
    override val varselId: UUID,
    override val value: String
) : OppgaveMelding