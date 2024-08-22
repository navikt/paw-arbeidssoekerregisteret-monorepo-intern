package no.nav.paw.arbeidssokerregisteret.application

interface Regler {
    val regler: List<Regel>
    val standardRegel: Regel
}
