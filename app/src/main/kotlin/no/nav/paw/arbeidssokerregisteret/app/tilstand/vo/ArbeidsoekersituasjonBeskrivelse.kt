package no.nav.paw.arbeidssokerregisteret.app.tilstand.vo


import no.nav.paw.arbeidssokerregisteret.app.tilstand.vo.ArbeidsoekersituasjonBeskrivelse.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.Beskrivelse as InternApiBeskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse as ApiBeskrivelse

enum class ArbeidsoekersituasjonBeskrivelse {
    UKJENT_VERDI,
    UDEFINERT,
    HAR_SAGT_OPP,
    HAR_BLITT_SAGT_OPP,
    ER_PERMITTERT,
    ALDRI_HATT_JOBB,
    IKKE_VAERT_I_JOBB_SISTE_2_AAR,
    AKKURAT_FULLFORT_UTDANNING,
    VIL_BYTTE_JOBB,
    USIKKER_JOBBSITUASJON,
    MIDLERTIDIG_JOBB,
    DELTIDSJOBB_VIL_MER,
    NY_JOBB,
    KONKURS,
    ANNET
}

fun arbeidsoekersituasjonBeskrivelse(beskrivelse: InternApiBeskrivelse): ArbeidsoekersituasjonBeskrivelse =
    when(beskrivelse) {
        InternApiBeskrivelse.UKJENT_VERDI -> UKJENT_VERDI
        InternApiBeskrivelse.UDEFINERT -> UDEFINERT
        InternApiBeskrivelse.HAR_SAGT_OPP -> HAR_SAGT_OPP
        InternApiBeskrivelse.HAR_BLITT_SAGT_OPP -> HAR_BLITT_SAGT_OPP
        InternApiBeskrivelse.ER_PERMITTERT -> ER_PERMITTERT
        InternApiBeskrivelse.ALDRI_HATT_JOBB -> ALDRI_HATT_JOBB
        InternApiBeskrivelse.IKKE_VEART_I_JOBB_SISTE_2_AAR -> IKKE_VAERT_I_JOBB_SISTE_2_AAR
        InternApiBeskrivelse.AKKURAT_FULLFORT_UTDANNING -> AKKURAT_FULLFORT_UTDANNING
        InternApiBeskrivelse.VIL_BYTTE_JOBB -> VIL_BYTTE_JOBB
        InternApiBeskrivelse.USIKKER_JOBBSITUASJON -> USIKKER_JOBBSITUASJON
        InternApiBeskrivelse.MIDLERTIDIG_JOBB -> MIDLERTIDIG_JOBB
        InternApiBeskrivelse.DELTIDSJOBB_VIL_MER -> DELTIDSJOBB_VIL_MER
        InternApiBeskrivelse.NY_JOBB -> NY_JOBB
        InternApiBeskrivelse.KONKURS -> KONKURS
        InternApiBeskrivelse.ANNET -> ANNET
    }

fun ArbeidsoekersituasjonBeskrivelse.api(): ApiBeskrivelse =
    when(this) {
        UKJENT_VERDI -> ApiBeskrivelse.UKJENT_VERDI
        UDEFINERT -> ApiBeskrivelse.UDEFINERT
        HAR_SAGT_OPP -> ApiBeskrivelse.HAR_SAGT_OPP
        HAR_BLITT_SAGT_OPP -> ApiBeskrivelse.HAR_BLITT_SAGT_OPP
        ER_PERMITTERT -> ApiBeskrivelse.ER_PERMITTERT
        ALDRI_HATT_JOBB -> ApiBeskrivelse.ALDRI_HATT_JOBB
        IKKE_VAERT_I_JOBB_SISTE_2_AAR -> ApiBeskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
        AKKURAT_FULLFORT_UTDANNING -> ApiBeskrivelse.AKKURAT_FULLFORT_UTDANNING
        VIL_BYTTE_JOBB -> ApiBeskrivelse.VIL_BYTTE_JOBB
        USIKKER_JOBBSITUASJON -> ApiBeskrivelse.USIKKER_JOBBSITUASJON
        MIDLERTIDIG_JOBB -> ApiBeskrivelse.MIDLERTIDIG_JOBB
        DELTIDSJOBB_VIL_MER -> ApiBeskrivelse.DELTIDSJOBB_VIL_MER
        NY_JOBB -> ApiBeskrivelse.NY_JOBB
        KONKURS -> ApiBeskrivelse.KONKURS
        ANNET -> ApiBeskrivelse.ANNET
    }