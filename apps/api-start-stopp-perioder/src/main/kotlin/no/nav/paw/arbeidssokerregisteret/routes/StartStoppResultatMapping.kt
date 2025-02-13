package no.nav.paw.arbeidssokerregisteret.routes

import arrow.core.Either
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.RoutingContext
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.*
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.*
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.*
import no.nav.paw.arbeidssokerregisteret.application.regler.*
import no.nav.paw.arbeidssokerregisteret.application.regler.UgyldigFeilretting
import no.nav.paw.collections.PawNonEmptyList
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Opplysning as ApiOpplysning


const val feilmeldingVedAvvist = "Avvist, se 'aarsakTilAvvisning' for detaljer"

suspend fun RoutingContext.respondWithV2(resultat: Either<PawNonEmptyList<Problem>, GrunnlagForGodkjenning>) =
    when (resultat) {
        is Either.Left -> respondWithErrorV2(resultat.value)
        is Either.Right -> call.respond(HttpStatusCode.NoContent)
    }

suspend fun RoutingContext.respondWithErrorV2(problemer: PawNonEmptyList<Problem>) {
    val (httpCode, feilkode) = problemer
        .toList()
        .firstOrNull { it.regel.id is AuthRegelId }
        ?.let { it.httpCode() to FeilV2.FeilKode.IKKE_TILGANG }
        ?: problemer.toList().firstOrNull { it.regel.id is DomeneRegelId }
            ?.let { it.httpCode() to FeilV2.FeilKode.AVVIST }
        ?: (HttpStatusCode.InternalServerError to FeilV2.FeilKode.UKJENT_FEIL)
    val melding = if (FeilV2.FeilKode.AVVIST == feilkode) {
        feilmeldingVedAvvist
    } else problemer.first.regel.id.beskrivelse
    call.respond(
        httpCode, FeilV2(
            melding = melding,
            feilKode = feilkode,
            aarsakTilAvvisning = if (feilkode == FeilV2.FeilKode.AVVIST) {
                AarsakTilAvvisningV2(
                    regler = problemer.map { ApiRegel(id = it.regel.id.apiRegelId(), beskrivelse = it.regel.id.beskrivelse) }.toList(),
                    detaljer = problemer.first.opplysninger.map(::opplysningTilApiOpplysning)
                )
            } else null
        )
    )
}

fun opplysningTilApiOpplysning(opplysning: Opplysning): ApiOpplysning =
    when (opplysning) {
        is DomeneOpplysning -> when (opplysning) {
            DomeneOpplysning.BarnFoedtINorgeUtenOppholdstillatelse -> ApiOpplysning.BARN_FOEDT_I_NORGE_UTEN_OPPHOLDSTILLATELSE
            DomeneOpplysning.BosattEtterFregLoven -> ApiOpplysning.BOSATT_ETTER_FREG_LOVEN
            DomeneOpplysning.Dnummer -> ApiOpplysning.DNUMMER
            DomeneOpplysning.ErDoed -> ApiOpplysning.DOED
            DomeneOpplysning.ErEuEoesStatsborger -> ApiOpplysning.ER_EU_EOES_STATSBORGER
            DomeneOpplysning.ErForhaandsgodkjent -> ApiOpplysning.FORHAANDSGODKJENT_AV_ANSATT
            DomeneOpplysning.ErGbrStatsborger -> ApiOpplysning.ER_GBR_STATSBORGER
            DomeneOpplysning.ErOver18Aar -> ApiOpplysning.ER_OVER_18_AAR
            DomeneOpplysning.ErSavnet -> ApiOpplysning.SAVNET
            DomeneOpplysning.ErUnder18Aar -> ApiOpplysning.ER_UNDER_18_AAR
            DomeneOpplysning.HarGyldigOppholdstillatelse -> ApiOpplysning.HAR_GYLDIG_OPPHOLDSTILLATELSE
            DomeneOpplysning.HarNorskAdresse -> ApiOpplysning.HAR_NORSK_ADRESSE
            DomeneOpplysning.HarUtenlandskAdresse -> ApiOpplysning.HAR_UTENLANDSK_ADRESSE
            DomeneOpplysning.IkkeBosatt -> ApiOpplysning.IKKE_BOSATT
            DomeneOpplysning.IkkeMuligAAIdentifisereSisteFlytting -> ApiOpplysning.IKKE_MULIG_AA_IDENTIFISERE_SISTE_FLYTTING
            DomeneOpplysning.IngenAdresseFunnet -> ApiOpplysning.INGEN_ADRESSE_FUNNET
            DomeneOpplysning.IngenFlytteInformasjon -> ApiOpplysning.INGEN_FLYTTE_INFORMASJON
            DomeneOpplysning.IngenInformasjonOmOppholdstillatelse -> ApiOpplysning.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE
            DomeneOpplysning.OpphoertIdentitet -> ApiOpplysning.OPPHOERT_IDENTITET
            DomeneOpplysning.OppholdstillatelseUtgaaatt -> ApiOpplysning.OPPHOLDSTILATELSE_UTGAATT
            DomeneOpplysning.PersonIkkeFunnet -> ApiOpplysning.PERSON_IKKE_FUNNET
            DomeneOpplysning.SisteFlyttingVarInnTilNorge -> ApiOpplysning.SISTE_FLYTTING_VAR_INN_TIL_NORGE
            DomeneOpplysning.SisteFlyttingVarUtAvNorge -> ApiOpplysning.SISTE_FLYTTING_VAR_UT_AV_NORGE
            DomeneOpplysning.TokenxPidIkkeFunnet -> ApiOpplysning.TOKENX_PID_IKKE_FUNNET
            DomeneOpplysning.UkjentFoedselsaar -> ApiOpplysning.UKJENT_FOEDSELSAAR
            DomeneOpplysning.UkjentFoedselsdato -> ApiOpplysning.UKJENT_FOEDSELSDATO
            DomeneOpplysning.UkjentForenkletFregStatus -> ApiOpplysning.UKJENT_FORENKLET_FREG_STATUS
            DomeneOpplysning.UkjentStatusForOppholdstillatelse -> ApiOpplysning.UKJENT_STATUS_FOR_OPPHOLDSTILLATELSE
            DomeneOpplysning.ErNorskStatsborger -> ApiOpplysning.ER_NORSK_STATSBORGER
            DomeneOpplysning.HarRegistrertAdresseIEuEoes -> ApiOpplysning.HAR_REGISTRERT_ADRESSE_I_EU_EOES
            DomeneOpplysning.ErFeilretting -> ApiOpplysning.ER_FEILRETTING
            DomeneOpplysning.UgyldigFeilretting -> ApiOpplysning.UGYLDIG_FEILRETTING
        }

        is AuthOpplysning -> when (opplysning) {
            AuthOpplysning.IkkeSammeSomInnloggerBruker -> ApiOpplysning.IKKE_SAMME_SOM_INNLOGGER_BRUKER
            AuthOpplysning.SammeSomInnloggetBruker -> ApiOpplysning.SAMME_SOM_INNLOGGET_BRUKER
            AuthOpplysning.TokenXPidIkkeFunnet -> ApiOpplysning.TOKENX_PID_IKKE_FUNNET
            AuthOpplysning.AnsattIkkeTilgang -> ApiOpplysning.ANSATT_IKKE_TILGANG
            AuthOpplysning.AnsattTilgang -> ApiOpplysning.ANSATT_TILGANG
            AuthOpplysning.IkkeAnsatt -> ApiOpplysning.IKKE_ANSATT
        }

        else -> ApiOpplysning.UKJENT_OPPLYSNING
    }

fun RegelId.apiRegelId(): ApiRegelId = when (this) {
    is AuthRegelId -> ApiRegelId.IKKE_TILGANG
    is DomeneRegelId -> when (this) {
        ForhaandsgodkjentAvAnsatt -> ApiRegelId.UKJENT_REGEL
        Over18AarOgBosattEtterFregLoven -> ApiRegelId.UKJENT_REGEL
        EuEoesStatsborgerOver18Aar -> ApiRegelId.UKJENT_REGEL
        ErStatsborgerILandMedAvtale -> ApiRegelId.UKJENT_REGEL
        Doed -> ApiRegelId.DOED
        Opphoert -> ApiRegelId.OPPHOERT_IDENTITET
        IkkeBosattINorgeIHenholdTilFolkeregisterloven -> ApiRegelId.IKKE_BOSATT_I_NORGE_I_HENHOLD_TIL_FOLKEREGISTERLOVEN
        IkkeFunnet -> ApiRegelId.IKKE_FUNNET
        Savnet -> ApiRegelId.SAVNET
        UkjentAlder -> ApiRegelId.UKJENT_ALDER
        Under18Aar -> ApiRegelId.UNDER_18_AAR
        EuEoesStatsborgerMenHarStatusIkkeBosatt -> ApiRegelId.ER_EU_EOES_STATSBORGER_MED_STATUS_IKKE_BOSATT
    }

    else -> ApiRegelId.UKJENT_REGEL
}

fun RegelId.apiRegel(): ApiRegel = ApiRegel(id = apiRegelId(), beskrivelse = beskrivelse)

fun Problem.httpCode(): HttpStatusCode = when (val regelId = this.regel.id) {
    is AuthRegelId -> regelId.httpCode()
    is DomeneRegelId -> regelId.httpCode()
    else -> HttpStatusCode.InternalServerError
}

fun AuthRegelId.httpCode(): HttpStatusCode = when (this) {
    AnsattHarTilgangTilBruker -> HttpStatusCode.OK
    EndreEgenBruker -> HttpStatusCode.OK
    AnsattIkkeTilgangTilBruker -> HttpStatusCode.Forbidden
    EndreForAnnenBruker -> HttpStatusCode.Forbidden
    IkkeAnsattOgForhaandsgodkjentAvAnsatt -> HttpStatusCode.BadRequest
    IkkeTilgang -> HttpStatusCode.Forbidden
    IkkeAnsattOgFeilretting -> HttpStatusCode.Forbidden
    UgyldigFeilretting -> HttpStatusCode.BadRequest
}

fun DomeneRegelId.httpCode(): HttpStatusCode = when (this) {
    ForhaandsgodkjentAvAnsatt -> HttpStatusCode.Accepted
    Over18AarOgBosattEtterFregLoven -> HttpStatusCode.Accepted
    EuEoesStatsborgerOver18Aar -> HttpStatusCode.Accepted
    ErStatsborgerILandMedAvtale -> HttpStatusCode.Accepted
    Doed -> HttpStatusCode.Forbidden
    Opphoert -> HttpStatusCode.Forbidden
    IkkeBosattINorgeIHenholdTilFolkeregisterloven -> HttpStatusCode.Forbidden
    IkkeFunnet -> HttpStatusCode.Forbidden
    Savnet -> HttpStatusCode.Forbidden
    UkjentAlder -> HttpStatusCode.Forbidden
    Under18Aar -> HttpStatusCode.Forbidden
    EuEoesStatsborgerMenHarStatusIkkeBosatt -> HttpStatusCode.Forbidden
}
