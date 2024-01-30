package no.nav.paw.arbeidssokerregisteret.evaluering

import no.nav.paw.arbeidssokerregisteret.evaluering.regler.Regel

enum class Attributt(val beskrivelse: String) {
    FORHAANDSGODKJENT_AV_ANSATT("Registrering er forhåndsgodkjent av NAV-ansatt"),
    SAMME_SOM_INNLOGGET_BRUKER("Start/stopp av periode er på samme bruker som er innlogget"),
    IKKE_SAMME_SOM_INNLOGGER_BRUKER("Start/stopp av periode er ikke på samme bruker som er innlogget"),
    ANSATT_IKKE_TILGANG("Innlogget bruker er en NAV-ansatt uten tilgang til bruker som start/stopp av periode utføres på"),
    ANSATT_TILGANG("Innlogget bruker er en NAV-ansatt med tilgang til bruker som start/stopp av periode utføres på"),
    IKKE_ANSATT("Innlogget bruker er ikke en NAV-ansatt"),
    ER_OVER_18_AAR("Personen start/stopp av periode utføres på er over 18 år"),
    ER_UNDER_18_AAR("Personen start/stopp av periode utføres på er under 18 år"),
    UKJENT_FOEDSELSDATO("Personen start/stopp av periode utføres på har ukjent fødselsdato"),
    UKJENT_FOEDSELSAAR("Personen start/stopp av periode utføres på har ukjent fødselsår"),
    TOKENX_PID_IKKE_FUNNET("Innlogget bruker er ikke en logget inn via TOKENX med PID(dvs ikke sluttbruker via ID-Porten)"),
    OPPHOERT_IDENTITET("Personen start/stopp av periode utføres på har opphørt identitet(annulert i Folkeregisteret)"),
    IKKE_BOSATT("Personen start/stopp av periode utføres på er ikke bosatt i Norge(eventuelt er innlytting anullert)"),
    DOED("Personen start/stopp av periode utføres på er død"),
    SAVNET("Personen start/stopp av periode utføres på er savnet"),
    HAR_NORSK_ADRESSE("Personen start/stopp av periode utføres på har norsk adresse"),
    HAR_UTENLANDSK_ADRESSE("Personen start/stopp av periode utføres på har utenlandsk adresse"),
    INGEN_ADRESSE_FUNNET("Personen start/stopp av periode utføres på har ingen adresse i våre systemer"),
    BOSATT_ETTER_FREG_LOVEN("Personen start/stopp av periode utføres på er bosatt i Norge i hendhold til Folkeregisterloven"),
    DNUMMER("Personen start/stopp av periode utføres på har D-nummer"),
    UKJENT_FORENKLET_FREG_STATUS("Personen start/stopp av periode utføres på har ukjent forenklet folkeregisterstatus"),
    HAR_GYLDIG_OPPHOLDSTILLATELSE("Personen start/stopp av periode utføres på har gyldig oppholdstillatelse"),
    OPPHOLDSTILATELSE_UTGAATT("Personen start/stopp av periode utføres på har oppholdstillatelse som er utgått"),
    BARN_FOEDT_I_NORGE_UTEN_OPPHOLDSTILLATELSE("Personen start/stopp av periode utføres på er født i Norge uten oppholdstillatelse"),
    INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE("Personen start/stopp av periode utføres på har ingen informasjon om oppholdstillatelse"),
    UKJENT_STATUS_FOR_OPPHOLDSTILLATELSE("Personen start/stopp av periode utføres på har ukjent status for oppholdstillatelse"),
    PERSON_IKKE_FUNNET("Personen start/stopp av periode utføres på er ikke funnet i våre systemer"),
    SISTE_FLYTTING_VAR_UT_AV_NORGE("Personen start/stopp av periode utføres på har en eller flere flyttinger hvorav den siste var ut av Norge"),
    SISTE_FLYTTING_VAR_INN_TIL_NORGE("Personen start/stopp av periode utføres på har en eller flere flyttinger hvorav den siste var inn til Norge"),
    IKKE_MULIG_AA_IDENTIFISERE_SISTE_FLYTTING("Personen start/stopp av periode utføres på har en eller flere flyttinger hvorav den siste ikke er mulig å identifisere"),
    INGEN_FLYTTE_INFORMASJON("Personen start/stopp av periode utføres på har ingen flytte informasjon"),
}

fun <R: Any> haandterResultat(
    regler: List<Regel>,
    resultat: Set<Attributt>,
    transformasjon: (String, List<Attributt>) -> R
): List<R> =
    regler.filter { (_, attributter) ->
        attributter.all { resultat.contains(it) }
    }.map { (melding, attributter) ->
        transformasjon(melding, attributter)
    }

