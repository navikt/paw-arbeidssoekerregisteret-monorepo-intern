package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import no.nav.paw.arbeidssokerregisteret.app.PeriodeTilstandV1

const val inkluder = true
fun Context.inkluderDersomIkkeRegistrertArbeidssøker() =
    this.gjeldeneTilstand == null

fun Context.inkluderDersomRegistrertArbeidssøker() =
    this.gjeldeneTilstand != null

fun PeriodeTilstandV1.erAvsluttet() = this.tilOgMed != null
