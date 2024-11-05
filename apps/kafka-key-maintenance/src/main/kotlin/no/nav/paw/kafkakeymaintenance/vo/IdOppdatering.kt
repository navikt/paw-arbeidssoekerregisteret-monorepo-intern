package no.nav.paw.kafkakeymaintenance.vo

import no.nav.paw.kafkakeygenerator.client.Alias
import no.nav.paw.kafkakeymaintenance.perioder.PeriodeRad

sealed interface IdOppdatering {}
data class ManuellIdOppdatering(
    val gjeldeneIdentitetsnummer: String?,
    val pdlIdentitetsnummer: List<String>,
    val lokaleAlias: List<Alias>,
    val perioder: List<PeriodeRad>
) : IdOppdatering

data class AutomatiskIdOppdatering(
    val oppdatertData: IdMap?,
    val frieIdentiteter: List<Alias>
) : IdOppdatering