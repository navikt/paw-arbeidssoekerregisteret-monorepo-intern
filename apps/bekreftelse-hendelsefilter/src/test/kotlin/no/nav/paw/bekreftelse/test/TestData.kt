package no.nav.paw.bekreftelse.test

import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.melding.v1.vo.Bruker
import no.nav.paw.bekreftelse.melding.v1.vo.BrukerType
import no.nav.paw.bekreftelse.melding.v1.vo.Metadata
import no.nav.paw.bekreftelse.melding.v1.vo.Svar
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start
import java.time.Instant
import java.util.*

object TestData {
    val bruker = Bruker(BrukerType.SLUTTBRUKER, "1234")
    val sendtInAv = Metadata(Instant.now(), bruker, "kilde", "aarsak")
    val svar = Svar(sendtInAv, Instant.now(), Instant.now(), false, true)
    val bekreftelse1 = Bekreftelse(
        UUID.randomUUID(),
        Bekreftelsesloesning.DAGPENGER,
        UUID.randomUUID(),
        svar
    )
    val bekreftelse2 = Bekreftelse(
        UUID.randomUUID(),
        Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
        UUID.randomUUID(),
        svar
    )
    val start = Start(1L, 1L)
    val paaVegneAv1 = PaaVegneAv(
        UUID.randomUUID(),
        no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.DAGPENGER,
        start
    )
    val paaVegneAv2 = PaaVegneAv(
        UUID.randomUUID(),
        no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
        start
    )
}