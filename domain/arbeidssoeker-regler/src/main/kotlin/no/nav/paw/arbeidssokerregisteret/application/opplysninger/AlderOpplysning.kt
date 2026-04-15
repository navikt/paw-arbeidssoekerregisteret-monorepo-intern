package no.nav.paw.arbeidssokerregisteret.application.opplysninger

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.ErOver18Aar
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.ErUnder18Aar
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.UkjentFoedselsaar
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.UkjentFoedselsdato
import no.nav.paw.pdl.graphql.generated.hentperson.Foedselsdato
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Month

private val fdatoLogger = LoggerFactory.getLogger("foedselsdato_logger")

fun alderOpplysning(foedselsdatoer: List<Foedselsdato>): Set<Opplysning> {
    if (foedselsdatoer.size > 1) {
        Span.current().addEvent("flere_foedselsdatoer", io.opentelemetry.api.common.Attributes.of(
            AttributeKey.stringKey("foedselsdatoer"), foedselsdatoer.joinToString(",") { it.metadata.master }
        ))
        fdatoLogger.info("Flere fødselsdatoer funnet: ${foedselsdatoer.joinToString(",") { it.metadata.master }}")
    }

    val resultater = foedselsdatoer.map(::alderOpplysning)

    if (resultater.isEmpty()) {
        return setOf(UkjentFoedselsaar, UkjentFoedselsdato)
    }

    if (resultater.all { ErOver18Aar in it }) {
        return resultater.first()
    }

    resultater.firstOrNull { ErUnder18Aar in it }?.let {
        return it
    }

    return resultater.firstOrNull { ErUnder18Aar !in it && ErOver18Aar !in it } ?: resultater.first()
}

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
