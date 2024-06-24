package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning

data class Problem(
    val opplysning: Iterable<Opplysning>,
    val regel: Regel
)

data class OK(
    val opplysning: Iterable<Opplysning>,
    val regel: Regel
)

fun ok(regel: Regel, opplysning: Iterable<Opplysning>): Either<Problem, OK> = OK(opplysning, regel).right()
fun problem(regel: Regel, opplysning: Iterable<Opplysning>): Either<Problem, OK> = Problem(opplysning, regel).left()