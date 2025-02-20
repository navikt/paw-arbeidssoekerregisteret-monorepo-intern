package no.nav.paw.arbeidssokerregisteret.routes

import arrow.core.Either
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.AarsakTilAvvisningV2
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiRegel
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiRegelId
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.FeilV2
import no.nav.paw.arbeidssokerregisteret.application.Doed
import no.nav.paw.arbeidssokerregisteret.application.DomeneRegelId
import no.nav.paw.arbeidssokerregisteret.application.ErStatsborgerILandMedAvtale
import no.nav.paw.arbeidssokerregisteret.application.EuEoesStatsborgerMenHarStatusIkkeBosatt
import no.nav.paw.arbeidssokerregisteret.application.EuEoesStatsborgerOver18Aar
import no.nav.paw.arbeidssokerregisteret.application.ForhaandsgodkjentAvAnsatt
import no.nav.paw.arbeidssokerregisteret.application.GrunnlagForGodkjenning
import no.nav.paw.arbeidssokerregisteret.application.IkkeBosattINorgeIHenholdTilFolkeregisterloven
import no.nav.paw.arbeidssokerregisteret.application.IkkeFunnet
import no.nav.paw.arbeidssokerregisteret.application.Opphoert
import no.nav.paw.arbeidssokerregisteret.application.Over18AarOgBosattEtterFregLoven
import no.nav.paw.arbeidssokerregisteret.application.Problem
import no.nav.paw.arbeidssokerregisteret.application.RegelId
import no.nav.paw.arbeidssokerregisteret.application.Savnet
import no.nav.paw.arbeidssokerregisteret.application.UkjentAlder
import no.nav.paw.arbeidssokerregisteret.application.Under18Aar
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.*
import no.nav.paw.arbeidssokerregisteret.application.regler.AnsattHarTilgangTilBruker
import no.nav.paw.arbeidssokerregisteret.application.regler.AnsattIkkeTilgangTilBruker
import no.nav.paw.arbeidssokerregisteret.application.regler.ValideringsRegelId
import no.nav.paw.arbeidssokerregisteret.application.regler.EndreEgenBruker
import no.nav.paw.arbeidssokerregisteret.application.regler.EndreForAnnenBruker
import no.nav.paw.arbeidssokerregisteret.application.regler.IkkeAnsattOgIkkeSystemOgFeilretting
import no.nav.paw.arbeidssokerregisteret.application.regler.IkkeAnsattOgForhaandsgodkjentAvAnsatt
import no.nav.paw.arbeidssokerregisteret.application.regler.IkkeTilgang
import no.nav.paw.arbeidssokerregisteret.application.regler.SystemHarIkkeTilgangTilBruker
import no.nav.paw.arbeidssokerregisteret.application.regler.SystemHarTilgangTilBruker
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
        .firstOrNull { it.regel.id is ValideringsRegelId }
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
                    regler = problemer.map {
                        ApiRegel(
                            id = it.regel.id.apiRegelId(),
                            beskrivelse = it.regel.id.beskrivelse
                        )
                    }.toList(),
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
            AuthOpplysning.SystemIkkeTilgang -> ApiOpplysning.SYSTEM_IKKE_TILGANG
            AuthOpplysning.SystemTilgang -> ApiOpplysning.SYSTEM_TILGANG
            AuthOpplysning.IkkeSystem -> ApiOpplysning.IKKE_SYSTEM
        }

        else -> ApiOpplysning.UKJENT_OPPLYSNING
    }

fun RegelId.apiRegelId(): ApiRegelId = when (this) {
    is ValideringsRegelId -> ApiRegelId.IKKE_TILGANG
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
    is ValideringsRegelId -> regelId.httpCode()
    is DomeneRegelId -> regelId.httpCode()
    else -> HttpStatusCode.InternalServerError
}

fun ValideringsRegelId.httpCode(): HttpStatusCode = when (this) {
    AnsattHarTilgangTilBruker -> HttpStatusCode.OK
    EndreEgenBruker -> HttpStatusCode.OK
    AnsattIkkeTilgangTilBruker -> HttpStatusCode.Forbidden
    EndreForAnnenBruker -> HttpStatusCode.Forbidden
    IkkeAnsattOgForhaandsgodkjentAvAnsatt -> HttpStatusCode.BadRequest
    IkkeTilgang -> HttpStatusCode.Forbidden
    IkkeAnsattOgIkkeSystemOgFeilretting -> HttpStatusCode.Forbidden
    UgyldigFeilretting -> HttpStatusCode.BadRequest
    SystemHarIkkeTilgangTilBruker -> HttpStatusCode.Forbidden
    SystemHarTilgangTilBruker -> HttpStatusCode.OK
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
