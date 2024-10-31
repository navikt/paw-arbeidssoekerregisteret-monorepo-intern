package no.nav.paw.arbeidssokerregisteret.application.opplysninger

import no.nav.paw.pdl.graphql.generated.hentperson.Foedselsdato
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*
import java.time.LocalDate
import java.time.Month

fun alderOpplysning(foedselsdato: Foedselsdato?): Set<Opplysning> {
    val dateOfBirth = foedselsdato?.foedselsdato?.let(LocalDate::parse)
    val lastDayInYearOfBirth =
        { foedselsdato?.foedselsaar?.let { foedselsAar -> LocalDate.of(foedselsAar, Month.DECEMBER, 31) } }
    val dob = dateOfBirth ?: lastDayInYearOfBirth()
    val preliminaryEvalResult = if (dateOfBirth != null) emptySet() else setOf(UkjentFoedselsdato)
    return if (dob != null) {
        if (isAtLeast18YearsAgo(dob)) {
            preliminaryEvalResult + ErOver18Aar
        } else {
            preliminaryEvalResult + ErUnder18Aar
        }
    } else {
        preliminaryEvalResult + UkjentFoedselsaar
    }
}

fun isAtLeast18YearsAgo(date: LocalDate) = isAtLeastYearsAgo(18, date)
fun isAtLeastYearsAgo(yearLimitInclusive: Long, date: LocalDate): Boolean =
    LocalDate.now().minusYears(yearLimitInclusive) >= date
