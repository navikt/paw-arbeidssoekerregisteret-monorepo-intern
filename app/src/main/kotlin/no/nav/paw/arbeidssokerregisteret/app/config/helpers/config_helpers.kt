package no.nav.paw.arbeidssokerregisteret.app.config.helpers

import java.time.Duration

inline fun <reified R> Map<String, String>.konfigVerdi(navn: String): R =
    get(navn)
        ?.let { value ->
            when (R::class) {
                String::class -> value as R
                Int::class -> value.toInt() as R
                Duration::class -> Duration.parse(value) as R
                Long::class -> value.toLong() as R
                else -> throw IllegalArgumentException("Navn: '$navn', Type '${R::class}' er ikke støttet")
            }
        } ?: if (null is R) {
        null as R
    } else {
        throw IllegalArgumentException("Den obligatoriske config parameteren '$navn' er ikke definert")
    }
