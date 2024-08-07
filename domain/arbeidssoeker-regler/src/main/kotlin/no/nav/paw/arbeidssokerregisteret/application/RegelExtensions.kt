package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning as HendelseOpplysning


operator fun String.invoke(
    vararg opplysninger: Opplysning,
    id: RegelId,
    vedTreff: (Regel, Iterable<Opplysning>) -> Either<Problem, OK>
) = Regel(
    id = id,
    beskrivelse = this,
    vedTreff = vedTreff,
    opplysninger = opplysninger.toList()
)

fun Regel.evaluer(samletOpplysning: Iterable<Opplysning>): Boolean = opplysninger.all { samletOpplysning.contains(it) }

fun List<Regel>.evaluer(opplysninger: Iterable<Opplysning>): Either<Problem, OK> =
    filter { regel -> regel.evaluer(opplysninger) }
        .map { regel -> regel.vedTreff(opplysninger) }
        .first()

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
