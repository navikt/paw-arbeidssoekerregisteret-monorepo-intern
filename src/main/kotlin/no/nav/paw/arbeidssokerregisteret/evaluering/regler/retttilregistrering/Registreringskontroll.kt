package no.nav.paw.arbeidssokerregisteret.evaluering.regler.retttilregistrering

import no.nav.paw.arbeidssokerregisteret.domain.Avvist
import no.nav.paw.arbeidssokerregisteret.domain.OK
import no.nav.paw.arbeidssokerregisteret.domain.Resultat
import no.nav.paw.arbeidssokerregisteret.domain.Uavklart
import no.nav.paw.arbeidssokerregisteret.evaluering.Fakta
import no.nav.paw.arbeidssokerregisteret.evaluering.regler.evaluer

fun sjekkOmRettTilRegistrering(samletFakta: Set<Fakta>): Resultat =
    sequenceOf(
        { harRettTilRegistreringEllerNull(samletFakta) },
        { harIkkeRettTilRegistreringEllerNull(samletFakta) },
        { Uavklart(melding = "Ingen regler funnet for evaluering", fakta = samletFakta) }
    ).mapNotNull { it() }.first()

private fun harIkkeRettTilRegistreringEllerNull(samletFakta: Set<Fakta>) = harIkkeRettTilRegistrering
    .filter { it.evaluer(samletFakta) }
    .map { (regelBeskrivelse, _) ->
        Avvist(
            melding = regelBeskrivelse,
            fakta = samletFakta
        )
    }.firstOrNull()

private fun harRettTilRegistreringEllerNull(samletFakta: Set<Fakta>) = harRettTilRegistrering
    .filter { it.evaluer(samletFakta) }
    .map { (regelBeskrivelse, _) ->
        OK(
            melding = regelBeskrivelse,
            fakta = samletFakta
        )
    }.firstOrNull()

