package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning as HendelseOpplysning


operator fun RegelId.invoke(
    vararg opplysninger: Opplysning,
    vedTreff: (Regel, Iterable<Opplysning>) -> Either<Problem, GrunnlagForGodkjenning>
) = Regel(
    id = this,
    vedTreff = vedTreff,
    opplysninger = opplysninger.toList()
)

fun Regel.evaluer(samletOpplysning: Iterable<Opplysning>): Boolean =
    opplysninger
        .filter { it !is Not<*> }
        .all { samletOpplysning.contains(it) } &&
    opplysninger
        .filterIsInstance<Not<*>>()
        .none { samletOpplysning.contains(it.value) }

/**
 * Evaluerer en liste med regler mot en liste med opplysninger. Returnerer første regel som evalueres til sann,
 * eller defaultRegel om ingen regler evalueres til sann.
 */
fun Regler.evaluer(
    opplysninger: Iterable<Opplysning>
): Either<Problem, GrunnlagForGodkjenning> =
    regler.filter { regel -> regel.evaluer(opplysninger) }
        .map { regel -> regel.vedTreff(opplysninger) }
        .firstOrNull() ?: standardRegel.vedTreff(opplysninger)

fun domeneOpplysningTilHendelseOpplysning(opplysning: DomeneOpplysning): HendelseOpplysning =
    when (opplysning) {
        DomeneOpplysning.BarnFoedtINorgeUtenOppholdstillatelse -> HendelseOpplysning.BARN_FOEDT_I_NORGE_UTEN_OPPHOLDSTILLATELSE
        DomeneOpplysning.BosattEtterFregLoven -> HendelseOpplysning.BOSATT_ETTER_FREG_LOVEN
        DomeneOpplysning.Dnummer -> HendelseOpplysning.DNUMMER
        DomeneOpplysning.ErDoed -> HendelseOpplysning.DOED
        DomeneOpplysning.ErEuEoesStatsborger -> HendelseOpplysning.ER_EU_EOES_STATSBORGER
        DomeneOpplysning.ErForhaandsgodkjent -> HendelseOpplysning.FORHAANDSGODKJENT_AV_ANSATT
        DomeneOpplysning.ErGbrStatsborger -> HendelseOpplysning.ER_GBR_STATSBORGER
        DomeneOpplysning.ErOver18Aar -> HendelseOpplysning.ER_OVER_18_AAR
        DomeneOpplysning.ErSavnet -> HendelseOpplysning.SAVNET
        DomeneOpplysning.ErUnder18Aar -> HendelseOpplysning.ER_UNDER_18_AAR
        DomeneOpplysning.HarGyldigOppholdstillatelse -> HendelseOpplysning.HAR_GYLDIG_OPPHOLDSTILLATELSE
        DomeneOpplysning.HarNorskAdresse -> HendelseOpplysning.HAR_NORSK_ADRESSE
        DomeneOpplysning.HarUtenlandskAdresse -> HendelseOpplysning.HAR_UTENLANDSK_ADRESSE
        DomeneOpplysning.IkkeBosatt -> HendelseOpplysning.IKKE_BOSATT
        DomeneOpplysning.IkkeMuligAAIdentifisereSisteFlytting -> HendelseOpplysning.IKKE_MULIG_AA_IDENTIFISERE_SISTE_FLYTTING
        DomeneOpplysning.IngenAdresseFunnet -> HendelseOpplysning.INGEN_ADRESSE_FUNNET
        DomeneOpplysning.IngenFlytteInformasjon -> HendelseOpplysning.INGEN_FLYTTE_INFORMASJON
        DomeneOpplysning.IngenInformasjonOmOppholdstillatelse -> HendelseOpplysning.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE
        DomeneOpplysning.OpphoertIdentitet -> HendelseOpplysning.OPPHOERT_IDENTITET
        DomeneOpplysning.OppholdstillatelseUtgaaatt -> HendelseOpplysning.OPPHOLDSTILATELSE_UTGAATT
        DomeneOpplysning.PersonIkkeFunnet -> HendelseOpplysning.PERSON_IKKE_FUNNET
        DomeneOpplysning.SisteFlyttingVarInnTilNorge -> HendelseOpplysning.SISTE_FLYTTING_VAR_INN_TIL_NORGE
        DomeneOpplysning.SisteFlyttingVarUtAvNorge -> HendelseOpplysning.SISTE_FLYTTING_VAR_UT_AV_NORGE
        DomeneOpplysning.TokenxPidIkkeFunnet -> HendelseOpplysning.TOKENX_PID_IKKE_FUNNET
        DomeneOpplysning.UkjentFoedselsaar -> HendelseOpplysning.UKJENT_FOEDSELSAAR
        DomeneOpplysning.UkjentFoedselsdato -> HendelseOpplysning.UKJENT_FOEDSELSDATO
        DomeneOpplysning.UkjentForenkletFregStatus -> HendelseOpplysning.UKJENT_FORENKLET_FREG_STATUS
        DomeneOpplysning.UkjentStatusForOppholdstillatelse -> HendelseOpplysning.UKJENT_STATUS_FOR_OPPHOLDSTILLATELSE
        DomeneOpplysning.ErNorskStatsborger -> HendelseOpplysning.ER_NORSK_STATSBORGER
        DomeneOpplysning.HarRegistrertAdresseIEuEoes -> HendelseOpplysning.HAR_REGISTRERT_ADRESSE_I_EU_EOES
    }

fun hendelseOpplysningTilDomeneOpplysninger(opplysning: HendelseOpplysning): DomeneOpplysning? =
    when (opplysning) {
        HendelseOpplysning.BARN_FOEDT_I_NORGE_UTEN_OPPHOLDSTILLATELSE -> DomeneOpplysning.BarnFoedtINorgeUtenOppholdstillatelse
        HendelseOpplysning.BOSATT_ETTER_FREG_LOVEN -> DomeneOpplysning.BosattEtterFregLoven
        HendelseOpplysning.DNUMMER -> DomeneOpplysning.Dnummer
        HendelseOpplysning.DOED -> DomeneOpplysning.ErDoed
        HendelseOpplysning.ER_EU_EOES_STATSBORGER -> DomeneOpplysning.ErEuEoesStatsborger
        HendelseOpplysning.FORHAANDSGODKJENT_AV_ANSATT -> DomeneOpplysning.ErForhaandsgodkjent
        HendelseOpplysning.ER_GBR_STATSBORGER -> DomeneOpplysning.ErGbrStatsborger
        HendelseOpplysning.ER_OVER_18_AAR -> DomeneOpplysning.ErOver18Aar
        HendelseOpplysning.SAVNET -> DomeneOpplysning.ErSavnet
        HendelseOpplysning.ER_UNDER_18_AAR -> DomeneOpplysning.ErUnder18Aar
        HendelseOpplysning.HAR_GYLDIG_OPPHOLDSTILLATELSE -> DomeneOpplysning.HarGyldigOppholdstillatelse
        HendelseOpplysning.HAR_NORSK_ADRESSE -> DomeneOpplysning.HarNorskAdresse
        HendelseOpplysning.HAR_UTENLANDSK_ADRESSE -> DomeneOpplysning.HarUtenlandskAdresse
        HendelseOpplysning.IKKE_BOSATT -> DomeneOpplysning.IkkeBosatt
        HendelseOpplysning.IKKE_MULIG_AA_IDENTIFISERE_SISTE_FLYTTING -> DomeneOpplysning.IkkeMuligAAIdentifisereSisteFlytting
        HendelseOpplysning.INGEN_ADRESSE_FUNNET -> DomeneOpplysning.IngenAdresseFunnet
        HendelseOpplysning.INGEN_FLYTTE_INFORMASJON -> DomeneOpplysning.IngenFlytteInformasjon
        HendelseOpplysning.INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE -> DomeneOpplysning.IngenInformasjonOmOppholdstillatelse
        HendelseOpplysning.OPPHOERT_IDENTITET -> DomeneOpplysning.OpphoertIdentitet
        HendelseOpplysning.OPPHOLDSTILATELSE_UTGAATT -> DomeneOpplysning.OppholdstillatelseUtgaaatt
        HendelseOpplysning.PERSON_IKKE_FUNNET -> DomeneOpplysning.PersonIkkeFunnet
        HendelseOpplysning.SISTE_FLYTTING_VAR_INN_TIL_NORGE -> DomeneOpplysning.SisteFlyttingVarInnTilNorge
        HendelseOpplysning.SISTE_FLYTTING_VAR_UT_AV_NORGE -> DomeneOpplysning.SisteFlyttingVarUtAvNorge
        HendelseOpplysning.TOKENX_PID_IKKE_FUNNET -> DomeneOpplysning.TokenxPidIkkeFunnet
        HendelseOpplysning.UKJENT_FOEDSELSAAR -> DomeneOpplysning.UkjentFoedselsaar
        HendelseOpplysning.UKJENT_FOEDSELSDATO -> DomeneOpplysning.UkjentFoedselsdato
        HendelseOpplysning.UKJENT_FORENKLET_FREG_STATUS -> DomeneOpplysning.UkjentForenkletFregStatus
        HendelseOpplysning.UKJENT_STATUS_FOR_OPPHOLDSTILLATELSE -> DomeneOpplysning.UkjentStatusForOppholdstillatelse
        HendelseOpplysning.ER_NORSK_STATSBORGER -> DomeneOpplysning.ErNorskStatsborger
        HendelseOpplysning.HAR_REGISTRERT_ADRESSE_I_EU_EOES -> DomeneOpplysning.HarRegistrertAdresseIEuEoes
        HendelseOpplysning.SAMME_SOM_INNLOGGET_BRUKER -> null
        HendelseOpplysning.IKKE_SAMME_SOM_INNLOGGER_BRUKER -> null
        HendelseOpplysning.ANSATT_IKKE_TILGANG -> null
        HendelseOpplysning.ANSATT_TILGANG -> null
        HendelseOpplysning.IKKE_ANSATT -> null
        HendelseOpplysning.UKJENT_OPPLYSNING -> null
    }
