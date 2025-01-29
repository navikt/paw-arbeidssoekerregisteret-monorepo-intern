package no.nav.paw.dolly.api.models

import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Annet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Helse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonMedDetaljer
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.OpplysningerOmArbeidssoeker as HendelseOpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Utdanning
import java.time.Instant
import java.util.UUID
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType as HendelseBrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata as HendelseMetadata

fun ArbeidssoekerregistreringRequest.toMetadata(): HendelseMetadata = HendelseMetadata(
    tidspunkt = Instant.now(),
    utfoertAv = Bruker(
        id = identitetsnummer,
        type = utfoertAv!!.asBrukerType()
    ),
    kilde = kilde!!,
    aarsak = aarsak!!
)

fun ArbeidssoekerregistreringRequest.toOpplysningerOmArbeidssoeker(metadata: Metadata): HendelseOpplysningerOmArbeidssoeker =
    HendelseOpplysningerOmArbeidssoeker(
        id = UUID.randomUUID(),
        metadata = metadata,
        utdanning = Utdanning(
            nus = nuskode!!,
            bestaatt = utdanningBestaatt?.asJaNeiVetikke(),
            godkjent = utdanningGodkjent?.asJaNeiVetikke()
        ),
        helse = Helse(
            helsetilstandHindrerArbeid = if (helsetilstandHindrerArbeid != false) JaNeiVetIkke.JA else JaNeiVetIkke.NEI,
        ),
        jobbsituasjon = Jobbsituasjon(
            beskrivelser = listOf(
                    JobbsituasjonMedDetaljer(
                        beskrivelse = jobbsituasjonBeskrivelse!!.toHendelseBeskrivelse(),
                        detaljer = jobbsituasjonDetaljer!!.toHendelseDetaljer()
                    )
                )
        ),
        annet = Annet(
            andreForholdHindrerArbeid = if (andreForholdHindrerArbeid != false) JaNeiVetIkke.JA else JaNeiVetIkke.NEI
        )
    )

fun Boolean.asJaNeiVetikke(): JaNeiVetIkke = when (this) {
    true -> JaNeiVetIkke.JA
    false -> JaNeiVetIkke.NEI
}

fun BrukerType.asBrukerType(): HendelseBrukerType = when (this) {
    BrukerType.UKJENT_VERDI -> HendelseBrukerType.UKJENT_VERDI
    BrukerType.UDEFINERT -> HendelseBrukerType.UDEFINERT
    BrukerType.VEILEDER -> HendelseBrukerType.VEILEDER
    BrukerType.SYSTEM -> HendelseBrukerType.SYSTEM
    BrukerType.SLUTTBRUKER -> HendelseBrukerType.SLUTTBRUKER
}

fun Beskrivelse.toHendelseBeskrivelse(): JobbsituasjonBeskrivelse = when (this) {
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

fun Detaljer.toHendelseDetaljer(): Map<String, String> =
    mapOf(
        "gjelder_fra_dato_iso8601" to gjelderFraDatoIso8601,
        "gjelder_til_dato_iso8601" to gjelderTilDatoIso8601,
        "stilling_styrk08" to stillingStyrk08,
        "stilling" to stilling,
        "prosent" to prosent,
        "siste_dag_med_loenn_iso8601" to sisteDagMedLoennIso8601,
        "siste_arbeidsdag_iso8601" to sisteArbeidsdagIso8601
    ).filterValues { it != null }
        .mapValues { it.value!!.toString() }
