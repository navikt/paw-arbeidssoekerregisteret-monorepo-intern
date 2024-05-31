package no.nav.paw.arbeidssokerregisteret.app.metrics

import java.time.Duration
import java.time.Instant

const val NOT_APPLICABLE = "NA"

fun percentageToBucket(prosent: String?): String =
    try {
        val verdi = prosent?.toDouble()?.toInt()
        when {
            null == verdi -> NOT_APPLICABLE
            verdi < 25 -> "under_25_prosent"
            verdi < 50 -> "25-50_prosent"
            verdi == 50 -> "50_prosent"
            verdi < 75 -> "51-75_prosent"
            verdi < 100 -> "76-99_prosent"
            else -> "100_prosent"
        }
    } catch (e: NumberFormatException) {
        NOT_APPLICABLE
    }

fun durationToBucket(timestamp: Instant): String {
    val duration = Duration.between(timestamp, Instant.now()).abs()
    return when {
        lessThan6Months(duration) -> "under_6_maaneder"
        lessThan1Year(duration) -> "6-12_maaneder"
        lessThan2Years(duration) -> "12-24_maaneder"
        else -> "mer_enn_24_maaneder"
    }
}

fun lessThan6Months(duration: Duration): Boolean {
    return duration < Duration.ofDays(183)
}

fun lessThan1Year(duration: Duration): Boolean {
    return duration < Duration.ofDays(365)
}

fun lessThan2Years(duration: Duration): Boolean {
    return duration < Duration.ofDays(365 * 2)
}