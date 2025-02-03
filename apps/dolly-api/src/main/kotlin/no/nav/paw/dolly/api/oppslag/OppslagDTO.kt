package no.nav.paw.dolly.api.oppslag


data class OpplysningerOmArbeidssoekerAggregertResponse(
    val opplysningerOmArbeidssoekerId: java.util.UUID,
    val periodeId: java.util.UUID,
    val sendtInnAv: MetadataResponse,
    val jobbsituasjon: List<BeskrivelseMedDetaljerResponse>,
    val utdanning: UtdanningResponse? = null,
    val helse: HelseResponse? = null,
    val annet: AnnetResponse? = null,
    val profilering: ProfileringResponse? = null
)

data class UtdanningResponse(
    val nus: String,
    val bestaatt: JaNeiVetIkke? = null,
    val godkjent: JaNeiVetIkke? = null
)

data class HelseResponse(
    val helsetilstandHindrerArbeid: JaNeiVetIkke
)

data class AnnetResponse(
    val andreForholdHindrerArbeid: JaNeiVetIkke? = null
)

data class ProfileringResponse(
    val profileringId: java.util.UUID,
    val periodeId: java.util.UUID,
    val opplysningerOmArbeidssoekerId: java.util.UUID,
    val sendtInnAv: MetadataResponse,
    val profilertTil: ProfileringsResultat,
    val jobbetSammenhengendeSeksAvTolvSisteManeder: Boolean? = null,
    val alder: Int? = null
)

enum class ProfileringsResultat(val value: String) {

    UKJENT_VERDI("UKJENT_VERDI"),

    UDEFINERT("UDEFINERT"),

    ANTATT_GODE_MULIGHETER("ANTATT_GODE_MULIGHETER"),

    ANTATT_BEHOV_FOR_VEILEDNING("ANTATT_BEHOV_FOR_VEILEDNING"),

    OPPGITT_HINDRINGER("OPPGITT_HINDRINGER");

}

enum class JaNeiVetIkke(val value: String) {

    JA("JA"),

    NEI("NEI"),

    VET_IKKE("VET_IKKE");

}

data class BekreftelseResponse(
    val periodeId: java.util.UUID,
    val bekreftelsesloesning: Bekreftelsesloesning,
    val svar: BekreftelseSvarResponse
)

enum class Bekreftelsesloesning(val value: String) {

    UKJENT_VERDI("UKJENT_VERDI"),

    ARBEIDSSOEKERREGISTERET("ARBEIDSSOEKERREGISTERET"),

    DAGPENGER("DAGPENGER");

}

data class BekreftelseSvarResponse(
    val sendtInnAv: MetadataResponse,
    val gjelderFra: java.time.Instant,
    val gjelderTil: java.time.Instant,
    val harJobbetIDennePerioden: Boolean,
    val vilFortsetteSomArbeidssoeker: Boolean
)

data class BeskrivelseMedDetaljerResponse(
    val beskrivelse: JobbSituasjonBeskrivelse,
    val detaljer: Map<String, String>
)

enum class JobbSituasjonBeskrivelse(val value: String) {

    UKJENT_VERDI("UKJENT_VERDI"),

    UDEFINERT("UDEFINERT"),

    HAR_SAGT_OPP("HAR_SAGT_OPP"),

    HAR_BLITT_SAGT_OPP("HAR_BLITT_SAGT_OPP"),

    ER_PERMITTERT("ER_PERMITTERT"),

    ALDRI_HATT_JOBB("ALDRI_HATT_JOBB"),

    IKKE_VAERT_I_JOBB_SISTE_2_AAR("IKKE_VAERT_I_JOBB_SISTE_2_AAR"),

    AKKURAT_FULLFORT_UTDANNING("AKKURAT_FULLFORT_UTDANNING"),

    VIL_BYTTE_JOBB("VIL_BYTTE_JOBB"),

    USIKKER_JOBBSITUASJON("USIKKER_JOBBSITUASJON"),

    MIDLERTIDIG_JOBB("MIDLERTIDIG_JOBB"),

    DELTIDSJOBB_VIL_MER("DELTIDSJOBB_VIL_MER"),

    NY_JOBB("NY_JOBB"),

    KONKURS("KONKURS"),

    ANNET("ANNET");

}

data class MetadataResponse(
    val tidspunkt: java.time.Instant,
    val utfoertAv: BrukerResponse,
    val kilde: String,
    val aarsak: String,
    val tidspunktFraKilde: TidspunktFraKildeResponse? = null
)

data class BrukerResponse(
    val type: BrukerType,
    val id: String
)

enum class BrukerType(val value: String) {

    UKJENT_VERDI("UKJENT_VERDI"),

    UDEFINERT("UDEFINERT"),

    VEILEDER("VEILEDER"),

    SYSTEM("SYSTEM"),

    SLUTTBRUKER("SLUTTBRUKER");

}

data class TidspunktFraKildeResponse(
    val tidspunkt: java.time.Instant,
    val avviksType: AvviksTypeResponse
)

enum class AvviksTypeResponse(val value: String) {

    UKJENT_VERDI("UKJENT_VERDI"),

    FORSINKELSE("FORSINKELSE"),

    RETTING("RETTING"),

    SLETTET("SLETTET"),

    TIDSPUNKT_KORRIGERT("TIDSPUNKT_KORRIGERT");

}


