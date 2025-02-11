package no.nav.paw.dolly.api.model

import io.ktor.server.plugins.BadRequestException
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Annet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Helse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonBeskrivelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.JobbsituasjonMedDetaljer
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Utdanning
import no.nav.paw.dolly.api.models.ArbeidssoekerregistreringRequest
import no.nav.paw.dolly.api.models.ArbeidssoekerregistreringResponse
import no.nav.paw.dolly.api.models.Brukertype
import no.nav.paw.dolly.api.models.Jobbsituasjonsbeskrivelse
import no.nav.paw.dolly.api.models.Jobbsituasjonsdetaljer
import no.nav.paw.dolly.api.models.TypeRequest
import java.time.Instant
import java.time.LocalDate
import java.util.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType as HendelseBrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata as HendelseMetadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.OpplysningerOmArbeidssoeker as HendelseOpplysningerOmArbeidssoeker

fun erGodkjentUtdanningsnivaa(nuskode: String?): Boolean =
    nuskode in setOf("3", "4", "5", "6", "7", "8") && nuskode != null

fun ArbeidssoekerregistreringRequest.medStandardverdier() =
    erGodkjentUtdanningsnivaa(nuskode).let { godkjentUtdanning ->
        copy(
            utfoertAv = utfoertAv ?: Brukertype.SLUTTBRUKER,
            kilde = kilde ?: "Dolly",
            aarsak = aarsak ?: "Registrering av arbeidssøker i Dolly",
            nuskode = nuskode ?: "3",
            utdanningBestaatt = if (!godkjentUtdanning) null else utdanningBestaatt ?: true,
            utdanningGodkjent = if (!godkjentUtdanning) null else utdanningGodkjent ?: true,
            jobbsituasjonsbeskrivelse = jobbsituasjonsbeskrivelse ?: Jobbsituasjonsbeskrivelse.HAR_BLITT_SAGT_OPP,
            jobbsituasjonsdetaljer = jobbsituasjonsdetaljer ?: Jobbsituasjonsdetaljer(
                stillingStyrk08 = "00",
                stillingstittel = "Annen stilling"
            ),
            helsetilstandHindrerArbeid = helsetilstandHindrerArbeid ?: false,
            andreForholdHindrerArbeid = andreForholdHindrerArbeid ?: false
        )
    }

fun ArbeidssoekerregistreringRequest.toMetadata(): HendelseMetadata = HendelseMetadata(
    tidspunkt = Instant.now(),
    utfoertAv = Bruker(
        id = identitetsnummer,
        type = utfoertAv!!.asHendelseBrukerType()
    ),
    kilde = kilde!!,
    aarsak = aarsak!!
)

fun hentAvsluttetMetadata() = HendelseMetadata(
    tidspunkt = Instant.now(),
    utfoertAv = Bruker(
        id = "null",
        type = HendelseBrukerType.SYSTEM
    ),
    kilde = "Dolly",
    aarsak = "Avsluttet i Dolly"
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
                    beskrivelse = jobbsituasjonsbeskrivelse!!.asHendelseJobbsituasjonsbeskrivelse(),
                    detaljer = jobbsituasjonsdetaljer!!.toHendelsesdetaljer()
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

fun Brukertype.asHendelseBrukerType(): HendelseBrukerType = when (this) {
    Brukertype.UKJENT_VERDI -> HendelseBrukerType.UKJENT_VERDI
    Brukertype.UDEFINERT -> HendelseBrukerType.UDEFINERT
    Brukertype.VEILEDER -> HendelseBrukerType.VEILEDER
    Brukertype.SYSTEM -> HendelseBrukerType.SYSTEM
    Brukertype.SLUTTBRUKER -> HendelseBrukerType.SLUTTBRUKER
}

fun Jobbsituasjonsbeskrivelse.asHendelseJobbsituasjonsbeskrivelse(): JobbsituasjonBeskrivelse = when (this) {
    Jobbsituasjonsbeskrivelse.UKJENT_VERDI -> JobbsituasjonBeskrivelse.UKJENT_VERDI
    Jobbsituasjonsbeskrivelse.UDEFINERT -> JobbsituasjonBeskrivelse.UDEFINERT
    Jobbsituasjonsbeskrivelse.HAR_SAGT_OPP -> JobbsituasjonBeskrivelse.HAR_SAGT_OPP
    Jobbsituasjonsbeskrivelse.HAR_BLITT_SAGT_OPP -> JobbsituasjonBeskrivelse.HAR_BLITT_SAGT_OPP
    Jobbsituasjonsbeskrivelse.ER_PERMITTERT -> JobbsituasjonBeskrivelse.ER_PERMITTERT
    Jobbsituasjonsbeskrivelse.ALDRI_HATT_JOBB -> JobbsituasjonBeskrivelse.ALDRI_HATT_JOBB
    Jobbsituasjonsbeskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR -> JobbsituasjonBeskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
    Jobbsituasjonsbeskrivelse.AKKURAT_FULLFORT_UTDANNING -> JobbsituasjonBeskrivelse.AKKURAT_FULLFORT_UTDANNING
    Jobbsituasjonsbeskrivelse.VIL_BYTTE_JOBB -> JobbsituasjonBeskrivelse.VIL_BYTTE_JOBB
    Jobbsituasjonsbeskrivelse.USIKKER_JOBBSITUASJON -> JobbsituasjonBeskrivelse.USIKKER_JOBBSITUASJON
    Jobbsituasjonsbeskrivelse.MIDLERTIDIG_JOBB -> JobbsituasjonBeskrivelse.MIDLERTIDIG_JOBB
    Jobbsituasjonsbeskrivelse.DELTIDSJOBB_VIL_MER -> JobbsituasjonBeskrivelse.DELTIDSJOBB_VIL_MER
    Jobbsituasjonsbeskrivelse.NY_JOBB -> JobbsituasjonBeskrivelse.NY_JOBB
    Jobbsituasjonsbeskrivelse.KONKURS -> JobbsituasjonBeskrivelse.KONKURS
    Jobbsituasjonsbeskrivelse.ANNET -> JobbsituasjonBeskrivelse.ANNET
}

fun Jobbsituasjonsdetaljer.toHendelsesdetaljer(): Map<String, String> =
    mapOf(
        "gjelder_fra_dato_iso8601" to gjelderFraDato,
        "gjelder_til_dato_iso8601" to gjelderTilDato,
        "stilling_styrk08" to stillingStyrk08,
        "stilling" to stillingstittel,
        "prosent" to stillingsprosent,
        "siste_dag_med_loenn_iso8601" to sisteDagMedLoenn,
        "siste_arbeidsdag_iso8601" to sisteArbeidsdag
    ).filterValues { it != null }
        .mapValues { it.value!!.toString() }


fun String.asTypeRequest(): TypeRequest =
    let { value ->
        TypeRequest.entries.find { it.name.equals(value, ignoreCase = true) }
            ?: throw BadRequestException("Ukjent type: $value")
    }

fun String?.asIdentitetsnummer(): String {
    requireNotNull(this) { throw BadRequestException("Mangler identitetsnummer") }
    require(erGyldigIdentitetsnummer()) { throw BadRequestException("Identitetsnummer må bestå av 11 sifre") }
    return this
}

fun String.erGyldigIdentitetsnummer() = matches(Regex("^\\d{11}$"))

fun OppslagResponse.toArbeidssoekerregistreringResponse(identitetsnummer: String): ArbeidssoekerregistreringResponse? {
    val opplysninger = opplysningerOmArbeidssoeker.firstOrNull() ?: return null
    val oppslagBeskrivelse = opplysninger.jobbsituasjon.firstOrNull()?.beskrivelse ?: return null
    val oppslagDetaljer = opplysninger.jobbsituasjon.firstOrNull()?.detaljer ?: return null
    val nuskode = opplysninger.utdanning?.nus ?: return null
    val utdanningBestaatt =
        if (opplysninger.utdanning.bestaatt != null) opplysninger.utdanning.bestaatt.value == "JA" else null
    val utdanningGodkjent =
        if (opplysninger.utdanning.godkjent != null) opplysninger.utdanning.godkjent.value == "JA" else null
    val helsetilstandHindrerArbeid = opplysninger.helse?.helsetilstandHindrerArbeid?.value == "JA"
    val andreForholdHindrerArbeid = opplysninger.annet?.andreForholdHindrerArbeid?.value == "JA"
    val utfoertAv = startet.utfoertAv.type.toBrukertype()
    val jobbsituasjonsbeskrivelse = oppslagBeskrivelse.toJobbsituasjonsbeskrivelse()
    val jobbsituasjonsdetaljer = oppslagDetaljer.toJobbsituasjonsdetaljer()

    return ArbeidssoekerregistreringResponse(
        identitetsnummer = identitetsnummer,
        utfoertAv = utfoertAv,
        kilde = startet.kilde,
        aarsak = startet.aarsak,
        nuskode = nuskode,
        utdanningBestaatt = utdanningBestaatt,
        utdanningGodkjent = utdanningGodkjent,
        jobbsituasjonsbeskrivelse = jobbsituasjonsbeskrivelse,
        jobbsituasjonsdetaljer = jobbsituasjonsdetaljer,
        helsetilstandHindrerArbeid = helsetilstandHindrerArbeid,
        andreForholdHindrerArbeid = andreForholdHindrerArbeid,
        registreringstidspunkt = startet.tidspunkt
    )
}

fun JobbSituasjonBeskrivelse.toJobbsituasjonsbeskrivelse(): Jobbsituasjonsbeskrivelse = when (this) {
    JobbSituasjonBeskrivelse.UKJENT_VERDI -> Jobbsituasjonsbeskrivelse.UKJENT_VERDI
    JobbSituasjonBeskrivelse.UDEFINERT -> Jobbsituasjonsbeskrivelse.UDEFINERT
    JobbSituasjonBeskrivelse.HAR_SAGT_OPP -> Jobbsituasjonsbeskrivelse.HAR_SAGT_OPP
    JobbSituasjonBeskrivelse.HAR_BLITT_SAGT_OPP -> Jobbsituasjonsbeskrivelse.HAR_BLITT_SAGT_OPP
    JobbSituasjonBeskrivelse.ER_PERMITTERT -> Jobbsituasjonsbeskrivelse.ER_PERMITTERT
    JobbSituasjonBeskrivelse.ALDRI_HATT_JOBB -> Jobbsituasjonsbeskrivelse.ALDRI_HATT_JOBB
    JobbSituasjonBeskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR -> Jobbsituasjonsbeskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
    JobbSituasjonBeskrivelse.AKKURAT_FULLFORT_UTDANNING -> Jobbsituasjonsbeskrivelse.AKKURAT_FULLFORT_UTDANNING
    JobbSituasjonBeskrivelse.VIL_BYTTE_JOBB -> Jobbsituasjonsbeskrivelse.VIL_BYTTE_JOBB
    JobbSituasjonBeskrivelse.USIKKER_JOBBSITUASJON -> Jobbsituasjonsbeskrivelse.USIKKER_JOBBSITUASJON
    JobbSituasjonBeskrivelse.MIDLERTIDIG_JOBB -> Jobbsituasjonsbeskrivelse.MIDLERTIDIG_JOBB
    JobbSituasjonBeskrivelse.DELTIDSJOBB_VIL_MER -> Jobbsituasjonsbeskrivelse.DELTIDSJOBB_VIL_MER
    JobbSituasjonBeskrivelse.NY_JOBB -> Jobbsituasjonsbeskrivelse.NY_JOBB
    JobbSituasjonBeskrivelse.KONKURS -> Jobbsituasjonsbeskrivelse.KONKURS
    JobbSituasjonBeskrivelse.ANNET -> Jobbsituasjonsbeskrivelse.ANNET
}

fun BrukerType.toBrukertype(): Brukertype = when (this) {
    BrukerType.UKJENT_VERDI -> Brukertype.UKJENT_VERDI
    BrukerType.UDEFINERT -> Brukertype.UDEFINERT
    BrukerType.VEILEDER -> Brukertype.VEILEDER
    BrukerType.SYSTEM -> Brukertype.SYSTEM
    BrukerType.SLUTTBRUKER -> Brukertype.SLUTTBRUKER
}

fun Map<String, String>.toJobbsituasjonsdetaljer(): Jobbsituasjonsdetaljer {
    return Jobbsituasjonsdetaljer(
        gjelderFraDato = this["gjelder_fra_dato_iso8601"]?.let { LocalDate.parse(it) },
        gjelderTilDato = this["gjelder_til_dato_iso8601"]?.let { LocalDate.parse(it) },
        stillingStyrk08 = this["stilling_styrk08"],
        stillingstittel = this["stilling"],
        stillingsprosent = this["prosent"],
        sisteDagMedLoenn = this["siste_dag_med_loenn_iso8601"]?.let { LocalDate.parse(it) },
        sisteArbeidsdag = this["siste_arbeidsdag_iso8601"]?.let { LocalDate.parse(it) }
    )
}

