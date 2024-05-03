package no.nav.paw.arbeidssokerregisteret.application.fakta

import no.nav.paw.arbeidssokerregisteret.application.Opplysning
import no.nav.paw.pdl.graphql.generated.hentperson.InnflyttingTilNorge
import no.nav.paw.pdl.graphql.generated.hentperson.UtflyttingFraNorge
import java.time.LocalDate
import java.time.LocalDateTime

fun utflyttingFakta(
    inn: InnflyttingTilNorge?,
    ut: UtflyttingFraNorge?
): Opplysning =
    when {
        inn == null && ut == null -> Opplysning.INGEN_FLYTTE_INFORMASJON
        inn != null && ut != null -> {
            val flyttinger = listOf(
                Flytting(inn = true, dato = inn.folkeregistermetadata?.ajourholdstidspunkt?.let(LocalDateTime::parse)?.toLocalDate()),
                Flytting(inn = false, dato = ut.utflyttingsdato?.let(LocalDate::parse))
            )
            when {
                flyttinger.distinctBy { it.dato }.size < 2 -> Opplysning.IKKE_MULIG_AA_IDENTIFISERE_SISTE_FLYTTING
                flyttinger.any { it.dato == null } -> Opplysning.IKKE_MULIG_AA_IDENTIFISERE_SISTE_FLYTTING
                else -> {
                    val sisteFlytting = flyttinger.maxByOrNull { it.dato!! }
                    if (sisteFlytting?.inn == true) Opplysning.SISTE_FLYTTING_VAR_INN_TIL_NORGE
                    else Opplysning.SISTE_FLYTTING_VAR_UT_AV_NORGE
                }
            }
        }
        inn != null -> Opplysning.SISTE_FLYTTING_VAR_INN_TIL_NORGE
        else -> Opplysning.SISTE_FLYTTING_VAR_UT_AV_NORGE
    }

private data class Flytting(
    val inn: Boolean,
    val dato: LocalDate?
)
