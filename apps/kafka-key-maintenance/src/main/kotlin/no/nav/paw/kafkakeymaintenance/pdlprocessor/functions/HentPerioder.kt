package no.nav.paw.kafkakeymaintenance.pdlprocessor.functions

import no.nav.paw.kafkakeymaintenance.perioder.Perioder
import no.nav.paw.kafkakeymaintenance.vo.AvviksMelding
import no.nav.paw.kafkakeymaintenance.vo.AvvvikOgPerioder

fun Perioder.hentPerioder(avviksMelding: AvviksMelding): AvvvikOgPerioder {
    val identiteter = avviksMelding.lokaleAlias
        .map { it.identitetsnummer } +
            avviksMelding.pdlIdentitetsnummer
    return AvvvikOgPerioder(
        avviksMelding = avviksMelding,
        perioder = get(identiteter.distinct())
    )
}