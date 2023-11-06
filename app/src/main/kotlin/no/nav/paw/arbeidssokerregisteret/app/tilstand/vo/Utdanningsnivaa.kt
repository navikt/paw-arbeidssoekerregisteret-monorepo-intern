package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo

import no.nav.paw.arbeidssokerregisteret.api.v1.Utdanningsnivaa as ApiUtdanningsnivaa
import no.nav.paw.arbeidssokerregisteret.intern.v1.Utdanningsnivaa as InternUtdanningsnivaa

enum class Utdanningsnivaa {
    UKJENT_VERDI,
    UDEFINERT,
    INGEN_UTDANNING,
    GRUNNSKOLE,
    VIDEREGAENDE_GRUNNUTDANNING,
    VIDEREGAENDE_FAGUTDANNING_SVENNEBREV,
    HOYERE_UTDANNING_1_TIL_4,
    HOYERE_UTDANNING_5_ELLER_MER,
}

fun utdaningsnivaa(utdanningsnivaa: InternUtdanningsnivaa): Utdanningsnivaa =
    when(utdanningsnivaa) {
        InternUtdanningsnivaa.UKJENT_VERDI -> Utdanningsnivaa.UKJENT_VERDI
        InternUtdanningsnivaa.UDEFINERT -> Utdanningsnivaa.UDEFINERT
        InternUtdanningsnivaa.GRUNNSKOLE -> Utdanningsnivaa.GRUNNSKOLE
        InternUtdanningsnivaa.VIDEREGAENDE_GRUNNUTDANNING -> Utdanningsnivaa.VIDEREGAENDE_GRUNNUTDANNING
        InternUtdanningsnivaa.VIDEREGAENDE_FAGBREV_SVENNEBREV -> Utdanningsnivaa.VIDEREGAENDE_FAGUTDANNING_SVENNEBREV
        InternUtdanningsnivaa.HOYERE_UTDANNING_1_TIL_4 -> Utdanningsnivaa.HOYERE_UTDANNING_1_TIL_4
        InternUtdanningsnivaa.HOYERE_UTDANNING_5_ELLER_MER -> Utdanningsnivaa.HOYERE_UTDANNING_5_ELLER_MER
        InternUtdanningsnivaa.INGEN_UTDANNING -> Utdanningsnivaa.INGEN_UTDANNING
    }

fun Utdanningsnivaa.api(): ApiUtdanningsnivaa =
    when(this) {
        Utdanningsnivaa.UKJENT_VERDI -> ApiUtdanningsnivaa.UKJENT_VERDI
        Utdanningsnivaa.UDEFINERT -> ApiUtdanningsnivaa.UDEFINERT
        Utdanningsnivaa.GRUNNSKOLE -> ApiUtdanningsnivaa.GRUNNSKOLE
        Utdanningsnivaa.VIDEREGAENDE_GRUNNUTDANNING -> ApiUtdanningsnivaa.VIDEREGAENDE_GRUNNUTDANNING
        Utdanningsnivaa.VIDEREGAENDE_FAGUTDANNING_SVENNEBREV -> ApiUtdanningsnivaa.VIDEREGAENDE_FAGBREV_SVENNEBREV
        Utdanningsnivaa.HOYERE_UTDANNING_1_TIL_4 -> ApiUtdanningsnivaa.HOYERE_UTDANNING_1_TIL_4
        Utdanningsnivaa.HOYERE_UTDANNING_5_ELLER_MER -> ApiUtdanningsnivaa.HOYERE_UTDANNING_5_ELLER_MER
        Utdanningsnivaa.INGEN_UTDANNING -> ApiUtdanningsnivaa.INGEN_UTDANNING
    }