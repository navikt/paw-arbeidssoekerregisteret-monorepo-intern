package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning

interface Problem {
    val opplysning: Iterable<Opplysning>
    val regel: Regel
}

data class SkalAvvises(
    override val opplysning: Iterable<Opplysning>,
    override val regel: Regel
): Problem

data class MuligGrunnlagForAvvisning(
    override val opplysning: Iterable<Opplysning>,
    override val regel: Regel
): Problem

data class GrunnlagForGodkjenning(
    val opplysning: Iterable<Opplysning>,
    val regel: Regel
)

fun grunnlagForGodkjenning(regel: Regel, opplysninger: Iterable<Opplysning>): Either<Problem, GrunnlagForGodkjenning> =
    GrunnlagForGodkjenning(opplysninger, regel).right()

fun skalAvises(regel: Regel, opplysninger: Iterable<Opplysning>): Either<Problem, GrunnlagForGodkjenning> =
    SkalAvvises(opplysninger, regel).left()

fun muligGrunnlagForAvvisning(regel: Regel, opplysninger: Iterable<Opplysning>): Either<Problem, GrunnlagForGodkjenning> =
    MuligGrunnlagForAvvisning(opplysninger, regel).left()
