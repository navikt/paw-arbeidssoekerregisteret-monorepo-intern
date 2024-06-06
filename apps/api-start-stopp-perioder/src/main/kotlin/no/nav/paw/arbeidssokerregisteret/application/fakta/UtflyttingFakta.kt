package no.nav.paw.arbeidssokerregisteret.application.fakta

import no.nav.paw.arbeidssokerregisteret.application.Opplysning
import no.nav.paw.pdl.graphql.generated.hentperson.InnflyttingTilNorge
import no.nav.paw.pdl.graphql.generated.hentperson.UtflyttingFraNorge
import java.time.LocalDate
import java.time.LocalDateTime

fun utflyttingFakta(
    inn: List<InnflyttingTilNorge>,
    ut: List<UtflyttingFraNorge>
): Opplysning =
    when {
        inn.isEmpty() && ut.isEmpty() -> Opplysning.INGEN_FLYTTE_INFORMASJON
        inn.isNotEmpty() && ut.isNotEmpty() -> {
            val flyttinger = inn.map {
                flyttingInn -> Flytting(inn = true, dato = flyttingInn.folkeregistermetadata?.ajourholdstidspunkt?.let(LocalDateTime::parse)?.toLocalDate())
            } + ut.map { utflytting ->
                Flytting(inn = false, dato = utflytting.utflyttingsdato?.let(LocalDate::parse))
            }
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
        inn.isNotEmpty() -> Opplysning.SISTE_FLYTTING_VAR_INN_TIL_NORGE
        else -> Opplysning.SISTE_FLYTTING_VAR_UT_AV_NORGE
    }

private data class Flytting(
    val inn: Boolean,
    val dato: LocalDate?
)
