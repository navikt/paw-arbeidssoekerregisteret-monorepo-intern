package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke as ApiJaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.intern.v1.JaNeiVetIkke as InternApiJaNeiVetIkke

enum class JaNeiVetIkke {
    JA,
    NEI,
    VET_IKKE,
}

fun jaNeiVetIkke(jaNeiVetIkke: InternApiJaNeiVetIkke): JaNeiVetIkke =
    when (jaNeiVetIkke) {
        InternApiJaNeiVetIkke.JA -> JaNeiVetIkke.JA
        InternApiJaNeiVetIkke.NEI -> JaNeiVetIkke.NEI
        InternApiJaNeiVetIkke.VET_IKKE -> JaNeiVetIkke.VET_IKKE
    }

fun JaNeiVetIkke.api(): ApiJaNeiVetIkke =
    when (this) {
        JaNeiVetIkke.JA -> ApiJaNeiVetIkke.JA
        JaNeiVetIkke.NEI -> ApiJaNeiVetIkke.NEI
        JaNeiVetIkke.VET_IKKE -> ApiJaNeiVetIkke.VET_IKKE
    }