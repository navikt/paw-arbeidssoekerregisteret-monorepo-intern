package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka

import arrow.core.*
import arrow.core.raise.Raise
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning


fun prosesser(
    regler: Regler,
    inngangsOpplysninger: Collection<Opplysning>,
    gjeldeneOpplysninger: Collection<Opplysning>
): ProsesseringsResultat =
    regler
        .evaluer(gjeldeneOpplysninger)
        .map { grunnlagForGodkjenning ->
            ProsesseringsResultat(
                grunnlag = setOf(grunnlagForGodkjenning.regel.id),
                periodeSkalAvsluttes = false,
                forhaandsgodkjenningSkalSlettes = inngangsOpplysninger
                    .contains(DomeneOpplysning.ErForhaandsgodkjent)
            )
        }.recover { gjeldeneProblemer ->
            val anseSomForhaandsGodkjent = inngangsOpplysninger.contains(DomeneOpplysning.ErForhaandsgodkjent)
                    || inngangsOpplysninger.isEmpty()
            if (anseSomForhaandsGodkjent) {
                when (val regResultat = registreringsResultat(
                    regler = regler,
                    inngangsOpplysninger = inngangsOpplysninger - DomeneOpplysning.ErForhaandsgodkjent
                )) {
                    is Either.Right -> raise(gjeldeneProblemer)
                    is Either.Left -> raiseIfNewProblem(gjeldeneProblemer, regResultat)
                }
            } else {
                raise(gjeldeneProblemer)
            }
        }.fold(
            {
                ProsesseringsResultat(
                    grunnlag = it.map { problem -> problem.regel.id }.toSet(),
                    periodeSkalAvsluttes = true,
                    forhaandsgodkjenningSkalSlettes = false
                )
            },
            { it }
        )

private fun Raise<NonEmptyList<Problem>>.raiseIfNewProblem(
    gjeldeneProblemer: NonEmptyList<Problem>,
    regResultat: Either.Left<NonEmptyList<Problem>>
): ProsesseringsResultat =
    gjeldeneProblemer.filterNot { gjeldeneProblem ->
        regResultat.value.any { it.regel.id == gjeldeneProblem.regel.id }
    }.let { nyeProblemer ->
        val head = nyeProblemer.firstOrNull()
        if (head == null) {
            ProsesseringsResultat(
                grunnlag = setOf(ForhaandsgodkjentAvAnsatt),
                periodeSkalAvsluttes = false,
                forhaandsgodkjenningSkalSlettes = false
            )
        } else {
            val noneEmptyNyeProblemer = nonEmptyListOf(
                head = head,
                *nyeProblemer.tail().toTypedArray()
            )
            raise(noneEmptyNyeProblemer)
        }
    }


fun registreringsResultat(
    regler: Regler,
    inngangsOpplysninger: Collection<Opplysning>
): Either<NonEmptyList<Problem>, GrunnlagForGodkjenning> {
    return if (inngangsOpplysninger.isEmpty()) {
        muligGrunnlagForAvvisning(
            regel = regler.regler.find { it.id == Under18Aar }!!,
            opplysninger = inngangsOpplysninger
        ).mapLeft(::nonEmptyListOf)
    } else {
        regler.evaluer(
            inngangsOpplysninger
        )
    }
}

data class ProsesseringsResultat(
    val grunnlag: Set<RegelId>,
    val periodeSkalAvsluttes: Boolean,
    val forhaandsgodkjenningSkalSlettes: Boolean
)