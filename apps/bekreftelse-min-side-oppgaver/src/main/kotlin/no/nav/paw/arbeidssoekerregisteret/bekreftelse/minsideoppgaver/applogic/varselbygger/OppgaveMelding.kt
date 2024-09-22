package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger

import java.util.UUID

interface OppgaveMelding {
    val varselId: UUID
    val value: String
}