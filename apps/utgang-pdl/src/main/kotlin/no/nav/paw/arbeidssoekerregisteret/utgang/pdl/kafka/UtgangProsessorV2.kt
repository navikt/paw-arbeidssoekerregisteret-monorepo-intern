package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.recover
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning
import no.nav.paw.felles.collection.PawNonEmptyList
import no.nav.paw.felles.collection.pawNonEmptyListOf


fun prosesser(
    regler: Regler,
    inngangsOpplysninger: Collection<Opplysning>,
    gjeldeneOpplysninger: Collection<Opplysning>
): ProsesseringsResultat =
    regler
        .evaluer(gjeldeneOpplysninger)
        .map { grunnlagForGodkjenning ->
            ProsesseringsResultat(
                grunnlag = pawNonEmptyListOf(grunnlagForGodkjenning.regel.id),
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
                    grunnlag = it.map { problem -> problem.regel.id },
                    periodeSkalAvsluttes = true,
                    forhaandsgodkjenningSkalSlettes = false
                )
            },
            { it }
        )

private fun Raise<PawNonEmptyList<Problem>>.raiseIfNewProblem(
    gjeldeneProblemer: PawNonEmptyList<Problem>,
    regResultat: Either.Left<PawNonEmptyList<Problem>>
): ProsesseringsResultat =
    gjeldeneProblemer.toList().filterNot { gjeldeneProblem ->
        regResultat.value.toList().any { it.regel.id == gjeldeneProblem.regel.id }
    }.let { nyeProblemer ->
        val head = nyeProblemer.firstOrNull()
        if (head == null) {
            ProsesseringsResultat(
                grunnlag = pawNonEmptyListOf(ForhaandsgodkjentAvAnsatt),
                periodeSkalAvsluttes = false,
                forhaandsgodkjenningSkalSlettes = false
            )
        } else {
            val noneEmptyNyeProblemer = pawNonEmptyListOf(
                head = head,
                nyeProblemer.drop(1)
            )
            raise(noneEmptyNyeProblemer)
        }
    }


fun registreringsResultat(
    regler: Regler,
    inngangsOpplysninger: Collection<Opplysning>
): Either<PawNonEmptyList<Problem>, GrunnlagForGodkjenning> {
    return if (inngangsOpplysninger.isEmpty()) {
        muligGrunnlagForAvvisning(
            regel = regler.regler.find { it.id == Under18Aar }!!,
            opplysninger = inngangsOpplysninger
        ).mapLeft(::pawNonEmptyListOf)
    } else {
        regler.evaluer(
            inngangsOpplysninger
        )
    }
}

data class ProsesseringsResultat(
    val grunnlag: PawNonEmptyList<RegelId>,
    val periodeSkalAvsluttes: Boolean,
    val forhaandsgodkjenningSkalSlettes: Boolean
)