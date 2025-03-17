package no.nav.paw.bekreftelsetjeneste.topology

import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand

data class BekreftelseProsesseringsResultat(
    val oppdatertTilstand: BekreftelseTilstand,
    val hendelser: List<BekreftelseHendelse>
)