package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

import no.nav.paw.arbeidssokerregisteret.api.v1.Arbeidsoekersituasjon as ApiArbeidsoekersituasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.Arbeidsoekersituasjon as InternApiArbeidsoekersituasjon

data class Arbeidsoekersituasjon(val beskrivelse: List<ArbeidsoekersituasjonBeskrivelse>)

fun arbeidsoekersituasjon(arbeidsoekersituasjon: InternApiArbeidsoekersituasjon): Arbeidsoekersituasjon =
    Arbeidsoekersituasjon(
        beskrivelse = arbeidsoekersituasjon.beskrivelser.map(::arbeidsoekersituasjonBeskrivelse)
    )

fun Arbeidsoekersituasjon.api(): ApiArbeidsoekersituasjon =
    ApiArbeidsoekersituasjon(
        beskrivelse.map { it.api() }
    )