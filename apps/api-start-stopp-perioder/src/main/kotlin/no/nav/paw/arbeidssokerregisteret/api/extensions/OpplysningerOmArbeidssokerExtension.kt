package no.nav.paw.arbeidssokerregisteret.api.extensions

import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.*
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.JobbsituasjonMedDetaljer.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.*
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.tilIdentitetsnummer
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Annet as AnnetInternal
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Helse as HelseInternal
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Jobbsituasjon as JobbsituasjonInternal
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonMedDetaljer as JobbsituasjonMedDetaljerInternal
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Utdanning as UtdanningInternal

fun OpplysningerRequest.getId(): Identitetsnummer = identitetsnummer.tilIdentitetsnummer()

fun Utdanning?.toInternalApi(): UtdanningInternal? =
    this?.let { utdanning ->
        UtdanningInternal(
            nus = utdanning.nus,
            bestaatt = utdanning.bestaatt.toNullableInternalApi(),
            godkjent = utdanning.godkjent.toNullableInternalApi(),
        )
    }

fun Helse?.toInternalApi(): HelseInternal? =
    this?.let {
        HelseInternal(
            helsetilstandHindrerArbeid = it.helsetilstandHindrerArbeid.toInternalApi(),
        )
    }

fun Annet?.toInternalApi(): AnnetInternal? =
    this?.let {
        AnnetInternal(
            andreForholdHindrerArbeid = it.andreForholdHindrerArbeid.toNullableInternalApi(),
        )
    }

fun Jobbsituasjon.toInternalApi(): JobbsituasjonInternal =
    JobbsituasjonInternal(
        beskrivelser = beskrivelser.map(JobbsituasjonMedDetaljer::toInternalApi)
    )

fun JobbsituasjonMedDetaljer.toInternalApi(): JobbsituasjonMedDetaljerInternal =
    JobbsituasjonMedDetaljerInternal(
        beskrivelse = beskrivelse.toInternalApi(),
        detaljer = detaljer.toInternalApi()
    )

fun Beskrivelse.toInternalApi(): JobbsituasjonBeskrivelse =
    when (this) {
        Beskrivelse.UKJENT_VERDI -> JobbsituasjonBeskrivelse.UKJENT_VERDI
        Beskrivelse.UDEFINERT -> JobbsituasjonBeskrivelse.UDEFINERT
        Beskrivelse.HAR_SAGT_OPP -> JobbsituasjonBeskrivelse.HAR_SAGT_OPP
        Beskrivelse.HAR_BLITT_SAGT_OPP -> JobbsituasjonBeskrivelse.HAR_BLITT_SAGT_OPP
        Beskrivelse.ER_PERMITTERT -> JobbsituasjonBeskrivelse.ER_PERMITTERT
        Beskrivelse.ALDRI_HATT_JOBB -> JobbsituasjonBeskrivelse.ALDRI_HATT_JOBB
        Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR -> JobbsituasjonBeskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
        Beskrivelse.AKKURAT_FULLFORT_UTDANNING -> JobbsituasjonBeskrivelse.AKKURAT_FULLFORT_UTDANNING
        Beskrivelse.VIL_BYTTE_JOBB -> JobbsituasjonBeskrivelse.VIL_BYTTE_JOBB
        Beskrivelse.USIKKER_JOBBSITUASJON -> JobbsituasjonBeskrivelse.USIKKER_JOBBSITUASJON
        Beskrivelse.MIDLERTIDIG_JOBB -> JobbsituasjonBeskrivelse.MIDLERTIDIG_JOBB
        Beskrivelse.DELTIDSJOBB_VIL_MER -> JobbsituasjonBeskrivelse.DELTIDSJOBB_VIL_MER
        Beskrivelse.NY_JOBB -> JobbsituasjonBeskrivelse.NY_JOBB
        Beskrivelse.KONKURS -> JobbsituasjonBeskrivelse.KONKURS
        Beskrivelse.ANNET -> JobbsituasjonBeskrivelse.ANNET
    }

fun Detaljer?.toInternalApi(): Map<String, String> =
    if (this == null) {
        emptyMap()
    } else {
        listOf(
            STILLING to stilling,
            STILLING_STYRK08 to stillingStyrk08,
            GJELDER_FRA_DATO to gjelderFraDatoIso8601?.toString(),
            GJELDER_TIL_DATO to gjelderTilDatoIso8601?.toString(),
            SISTE_ARBEIDSDAG to sisteArbeidsdagIso8601?.toString(),
            SISTE_DAG_MED_LOENN to sisteDagMedLoennIso8601?.toString(),
            PROSENT to prosent
        )
            .mapNotNull { (key, value) -> value?.let { key to it } }
            .toMap()
    }
