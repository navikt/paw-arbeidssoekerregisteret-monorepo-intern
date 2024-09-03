package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning

data class Regel(
    val id: RegelId,
    /**
     * Kritieries som må være oppfylt for at regelen skal være sann
     */
    val kritierier: List<Condition>,

    private val vedTreff: (Regel, Iterable<Opplysning>) -> Either<Problem, GrunnlagForGodkjenning>
) {
    fun vedTreff(opplysning: Iterable<Opplysning>): Either<Problem, GrunnlagForGodkjenning> = vedTreff(this, opplysning)
}