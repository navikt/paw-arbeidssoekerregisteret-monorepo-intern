package no.nav.paw.arbeidssoekerregisteret.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicLong

fun MeterRegistry.tellMottatteOpplysninger() {
    counter(
        "paw_antall_mottatte_meldinger_total",
        Tags.of(
            Tag.of("meldingstype", "opplysninger-om-arbeidssoeker")
        )
    ).increment()
}

fun MeterRegistry.antallLagredeOpplysningerTotal(antallReference: AtomicLong) {
    gauge(
        "paw_antall_lagrede_meldinger",
        Tags.of(
            Tag.of("meldingstype", "opplysninger-om-arbeidssoeker")
        ),
        antallReference
    ) {
        antallReference.get().toDouble()
    }
}

fun MeterRegistry.antallLagredeOpplysningerSumPerPeriode(timestamp: Instant, antallReference: AtomicLong) {
    val zonedDateTime = timestamp.atZone(ZoneId.systemDefault())
    gauge(
        "paw_antall_lagrede_meldinger_sum_per_tidsperiode",
        Tags.of(
            Tag.of("meldingstype", "opplysninger-om-arbeidssoeker"),
            Tag.of("minutt", "${zonedDateTime.minute}")
        ),
        antallReference
    ) {
        antallReference.get().toDouble()
    }
}
