package no.nav.paw.arbeidssokerregisteret.application.fakta

import no.nav.paw.arbeidssokerregisteret.application.Fakta
import no.nav.paw.pdl.graphql.generated.hentperson.InnflyttingTilNorge
import no.nav.paw.pdl.graphql.generated.hentperson.UtflyttingFraNorge
import java.time.LocalDate
import java.time.LocalDateTime

fun utflyttingFakta(
    inn: InnflyttingTilNorge?,
    ut: UtflyttingFraNorge?
): Fakta =
    when {
        inn == null && ut == null -> Fakta.INGEN_FLYTTE_INFORMASJON
        inn != null && ut != null -> {
            val flyttinger = listOf(
                Flytting(inn = true, dato = inn.folkeregistermetadata?.ajourholdstidspunkt?.let(LocalDateTime::parse)?.toLocalDate()),
                Flytting(inn = false, dato = ut.utflyttingsdato?.let(LocalDate::parse))
            )
            when {
                flyttinger.distinctBy { it.dato }.size < 2 -> Fakta.IKKE_MULIG_AA_IDENTIFISERE_SISTE_FLYTTING
                flyttinger.any { it.dato == null } -> Fakta.IKKE_MULIG_AA_IDENTIFISERE_SISTE_FLYTTING
                else -> {
                    val sisteFlytting = flyttinger.maxByOrNull { it.dato!! }
                    if (sisteFlytting?.inn == true) Fakta.SISTE_FLYTTING_VAR_INN_TIL_NORGE
                    else Fakta.SISTE_FLYTTING_VAR_UT_AV_NORGE
                }
            }
        }
        inn != null -> Fakta.SISTE_FLYTTING_VAR_INN_TIL_NORGE
        else -> Fakta.SISTE_FLYTTING_VAR_UT_AV_NORGE
    }

private data class Flytting(
    val inn: Boolean,
    val dato: LocalDate?
)
