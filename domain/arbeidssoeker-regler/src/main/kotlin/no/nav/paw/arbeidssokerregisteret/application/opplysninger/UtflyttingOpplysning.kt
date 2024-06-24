package no.nav.paw.arbeidssokerregisteret.application.opplysninger

import no.nav.paw.pdl.graphql.generated.hentperson.InnflyttingTilNorge
import no.nav.paw.pdl.graphql.generated.hentperson.UtflyttingFraNorge
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*

fun utflyttingOpplysning(
    inn: List<InnflyttingTilNorge>,
    ut: List<UtflyttingFraNorge>
): Opplysning =
    when {
        inn.isEmpty() && ut.isEmpty() -> IngenFlytteInformasjon
        inn.isNotEmpty() && ut.isNotEmpty() -> {
            val flyttinger = inn.map {
                flyttingInn -> Flytting(inn = true, dato = flyttingInn.folkeregistermetadata?.ajourholdstidspunkt?.let(LocalDateTime::parse)?.toLocalDate())
            } + ut.map { utflytting ->
                Flytting(inn = false, dato = utflytting.utflyttingsdato?.let(LocalDate::parse))
            }
            when {
                flyttinger.distinctBy { it.dato }.size < 2 -> IkkeMuligAAIdentifisereSisteFlytting
                flyttinger.any { it.dato == null } -> IkkeMuligAAIdentifisereSisteFlytting
                else -> {
                    val sisteFlytting = flyttinger.maxByOrNull { it.dato!! }
                    if (sisteFlytting?.inn == true) SisteFlyttingVarInnTilNorge
                    else SisteFlyttingVarUtAvNorge
                }
            }
        }
        inn.isNotEmpty() -> SisteFlyttingVarInnTilNorge
        else -> SisteFlyttingVarUtAvNorge
    }

private data class Flytting(
    val inn: Boolean,
    val dato: LocalDate?
)
