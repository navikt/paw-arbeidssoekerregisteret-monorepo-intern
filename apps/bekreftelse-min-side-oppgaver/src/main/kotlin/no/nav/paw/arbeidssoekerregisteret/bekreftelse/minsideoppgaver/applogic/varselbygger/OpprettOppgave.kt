package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger

import java.util.*

data class OpprettOppgave(
    override val varselId: UUID,
    override val value: String
) : OppgaveMelding