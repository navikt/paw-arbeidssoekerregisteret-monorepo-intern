package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

import no.nav.paw.arbeidssokerregisteret.api.v1.Arbeidsoekersituasjon as ApiArbeidsoekersituasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.Arbeidsoekersituasjon as InternApiArbeidsoekersituasjon

data class Arbeidsoekersituasjon(val beskrivelser: List<Element>)

fun arbeidsoekersituasjon(arbeidsoekersituasjon: InternApiArbeidsoekersituasjon): Arbeidsoekersituasjon =
    Arbeidsoekersituasjon(
        beskrivelser = arbeidsoekersituasjon.beskrivelser.map(::element)
    )

fun Arbeidsoekersituasjon.api(): ApiArbeidsoekersituasjon =
    ApiArbeidsoekersituasjon(
        beskrivelser.map(Element::api)
    )