package no.nav.paw.arbeidssokerregisteret.application.fakta

import no.nav.paw.arbeidssokerregisteret.application.Opplysning
import no.nav.paw.pdl.graphql.generated.hentperson.Foedsel
import java.time.LocalDate
import java.time.Month

fun alderFakta(foedsel: Foedsel?): Set<Opplysning> {
    val dateOfBirth = foedsel?.foedselsdato?.let(LocalDate::parse)
    val lastDayInYearOfBirth = { foedsel?.foedselsaar?.let { foedselsAar -> LocalDate.of(foedselsAar, Month.DECEMBER, 31) } }
    val dob = dateOfBirth ?: lastDayInYearOfBirth()
    val preliminaryEvalResult = if (dateOfBirth != null) emptySet() else setOf(Opplysning.UKJENT_FOEDSELSDATO)
    return if (dob != null) {
        if (isAtLeast18YearsAgo(dob)) {
            preliminaryEvalResult + Opplysning.ER_OVER_18_AAR
        } else {
            preliminaryEvalResult + Opplysning.ER_UNDER_18_AAR
        }
    } else {
        preliminaryEvalResult + Opplysning.UKJENT_FOEDSELSAAR
    }
}

fun isAtLeast18YearsAgo(date: LocalDate) = isAtLeastYearsAgo(18, date)
fun isAtLeastYearsAgo(yearLimitInclusive: Long, date: LocalDate): Boolean =
    LocalDate.now().minusYears(yearLimitInclusive) >= date
