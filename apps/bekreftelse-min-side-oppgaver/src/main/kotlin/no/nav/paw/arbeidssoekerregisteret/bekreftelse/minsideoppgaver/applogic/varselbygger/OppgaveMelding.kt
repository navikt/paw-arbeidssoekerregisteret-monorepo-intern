package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger

import java.util.UUID

/**
 * Builderen fra 'no.nav.tms.varsel:kotlin-builder:1.1.0' returnerer bare en json String. Dette interface brukes til
 * å wrappe forskjellige meldingstyper.
  */
interface OppgaveMelding {
    val varselId: UUID
    val value: String
}
