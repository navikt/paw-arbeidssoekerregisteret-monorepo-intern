package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning

data class Regel(
    val id: RegelId,
    /**
     * Beskrivelse av regelen
     */
    val beskrivelse: String,
    /**
     * Opplysninger som må være tilstede for at regelen skal være sann
     */
    val opplysninger: List<Opplysning>,

    private val vedTreff: (Regel, Iterable<Opplysning>) -> Either<Problem, OK>
) {
    fun vedTreff(opplysning: Iterable<Opplysning>): Either<Problem, OK> = vedTreff(this, opplysning)
}