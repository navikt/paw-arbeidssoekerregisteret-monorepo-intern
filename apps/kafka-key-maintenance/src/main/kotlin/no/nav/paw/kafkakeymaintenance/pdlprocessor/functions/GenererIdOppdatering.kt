package no.nav.paw.kafkakeymaintenance.pdlprocessor.functions

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.tail
import no.nav.paw.kafkakeygenerator.client.Alias
import no.nav.paw.kafkakeymaintenance.vo.IdOppdatering
import no.nav.paw.kafkakeymaintenance.vo.ManuellIdOppdatering
import no.nav.paw.kafkakeymaintenance.perioder.PeriodeRad
import no.nav.paw.kafkakeymaintenance.vo.AvviksMelding
import no.nav.paw.kafkakeymaintenance.vo.AvvvikOgPerioder
import java.time.Instant

fun genererIdOppdatering(avvikOgPerioder: AvvvikOgPerioder): IdOppdatering {
    val (avvik, perioder) = avvikOgPerioder
    val periode = perioder.firstOrNull()
    return if (periode == null) {
        genererAutomatiskIdOppdatering(avvik)
    } else {
        genererIdOppdatering(avvik, nonEmptyListOf(periode, *perioder.tail().toTypedArray()))
    }
}

fun genererIdOppdatering(avvik: AvviksMelding, perioder: NonEmptyList<PeriodeRad>): IdOppdatering {
    val aktivPerioder = perioder.filter(PeriodeRad::erAktiv)
    return when (aktivPerioder.size) {
        0, 1 -> genererAutomatiskIdOppdatering(avvik, perioder.maxBy { it.til ?: Instant.MAX })
        else -> ManuellIdOppdatering(
            gjeldeneIdentitetsnummer = avvik.gjeldeneIdentitetsnummer,
            pdlIdentitetsnummer = avvik.lokaleAliasSomSkalPekePaaPdlPerson().map(Alias::identitetsnummer),
            lokaleAlias = avvik.lokaleAlias,
            perioder = perioder
        )
    }
}
