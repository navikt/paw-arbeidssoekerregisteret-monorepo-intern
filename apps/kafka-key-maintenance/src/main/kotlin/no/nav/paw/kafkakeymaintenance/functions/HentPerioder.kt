package no.nav.paw.kafkakeymaintenance.functions

import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import no.nav.paw.kafkakeymaintenance.perioder.periodeRad
import no.nav.paw.kafkakeymaintenance.vo.AvviksMelding
import no.nav.paw.kafkakeymaintenance.vo.AvvvikOgPerioder

fun TransactionContext.hentPerioder(avviksMelding: AvviksMelding): AvvvikOgPerioder {
    val perioder = avviksMelding
        .pdlIdentitetsnummer
        .plus(avviksMelding.lokaleAlias.map { it.identitetsnummer })
        .distinct()
        .mapNotNull(::periodeRad)
    return AvvvikOgPerioder(avviksMelding, perioder)
}