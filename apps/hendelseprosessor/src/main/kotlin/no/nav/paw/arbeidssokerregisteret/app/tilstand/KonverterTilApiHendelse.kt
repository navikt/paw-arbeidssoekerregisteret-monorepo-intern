package no.nav.paw.arbeidssokerregisteret.app.tilstand

import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker as ApiBruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType as ApiBrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse as ApiHelse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke as ApiJaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as ApiMetadata
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker as ApiOpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning as ApiUtdanning
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon as ApiJobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse as ApiBeskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet as ApiAnnet
import no.nav.paw.arbeidssokerregisteret.api.v1.BeskrivelseMedDetaljer as ApiBeskrivelseMedDetaljer

import java.util.*

fun Bruker.api(): ApiBruker = ApiBruker(type.api(), id)

fun BrukerType.api(): ApiBrukerType =
    when (this) {
        BrukerType.VEILEDER -> ApiBrukerType.VEILEDER
        BrukerType.SYSTEM -> ApiBrukerType.SYSTEM
        BrukerType.SLUTTBRUKER -> ApiBrukerType.SLUTTBRUKER
        BrukerType.UKJENT_VERDI -> ApiBrukerType.UKJENT_VERDI
        BrukerType.UDEFINERT -> ApiBrukerType.UDEFINERT
    }


fun Helse.api(): ApiHelse =
    ApiHelse(
        helsetilstandHindrerArbeid.api()
    )


fun JaNeiVetIkke.api(): ApiJaNeiVetIkke =
    when (this) {
        JaNeiVetIkke.JA -> ApiJaNeiVetIkke.JA
        JaNeiVetIkke.NEI -> ApiJaNeiVetIkke.NEI
        JaNeiVetIkke.VET_IKKE -> ApiJaNeiVetIkke.VET_IKKE
    }

fun Metadata.api(): ApiMetadata =
    ApiMetadata(
        tidspunkt,
        utfoertAv.api(),
        kilde,
        aarsak
    )
fun OpplysningerOmArbeidssoeker.api(periodeId: UUID, metadata: Metadata): ApiOpplysningerOmArbeidssoeker =
    ApiOpplysningerOmArbeidssoeker(
        id,
        periodeId,
        metadata.api(),
        utdanning?.api(),
        helse?.api(),
        jobbsituasjon.api(),
        annet?.api()
    )

fun Utdanning.api(): ApiUtdanning =
    ApiUtdanning(
        nus,
        bestaatt?.api(),
        godkjent?.api(),
    )

fun Jobbsituasjon.api(): ApiJobbsituasjon =
    ApiJobbsituasjon(
        beskrivelser.map(JobbsituasjonMedDetaljer::api)
    )

fun JobbsituasjonMedDetaljer.api(): ApiBeskrivelseMedDetaljer =
    ApiBeskrivelseMedDetaljer(
        beskrivelse.api(),
        detaljer
    )

fun JobbsituasjonBeskrivelse.api(): ApiBeskrivelse =
    when (this) {
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

fun Annet.api(): ApiAnnet = ApiAnnet(
    andreForholdHindrerArbeid?.api()
)