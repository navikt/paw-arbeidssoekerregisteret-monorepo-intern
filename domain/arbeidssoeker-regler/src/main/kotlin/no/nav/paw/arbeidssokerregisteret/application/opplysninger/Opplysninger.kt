package no.nav.paw.arbeidssokerregisteret.application.opplysninger

import no.nav.paw.arbeidssokerregisteret.application.Condition

interface Opplysning: Condition {
    val id: String
    val beskrivelse: String
    override fun eval(opplysninger: Iterable<Opplysning>): Boolean = this in opplysninger
}

infix operator fun Opplysning.plus(opplysning: Opplysning): Set<Opplysning> = setOf(this, opplysning)
sealed interface DomeneOpplysning: Opplysning {

    data object ErForhaandsgodkjent : DomeneOpplysning, Effect.Positive {
        override val id = "FORHAANDSGODKJENT_AV_ANSATT"
        override val beskrivelse = "Registrering er forhåndsgodkjent av NAV-ansatt"
    }

    data object ErFeilretting: DomeneOpplysning, Effect.Neutral {
        override val id = "FEILRETTING"
        override val beskrivelse = "Operasjonen er en feilretting"
    }

    data object ErOver18Aar : DomeneOpplysning, Effect.Positive {
        override val id = "ER_OVER_18_AAR"
        override val beskrivelse = "Personen start/stopp av periode utføres på er over 18 år"
    }

    data object ErUnder18Aar : DomeneOpplysning, Effect.Negative {
        override val id = "ER_UNDER_18_AAR"
        override val beskrivelse = "Personen start/stopp av periode utføres på er under 18 år"
    }

    data object ErEuEoesStatsborger : DomeneOpplysning, Effect.Positive {
        override val id = "ER_EU_EOES_STATSBORGER"
        override val beskrivelse = "Personen start/stopp av periode utføres på er EØS/EU statsborger"
    }

    data object ErGbrStatsborger : DomeneOpplysning, Effect.Positive {
        override val id = "ER_GBR_STATSBORGER"
        override val beskrivelse = "Personen start/stopp av periode utføres på er britisk statsborger"
    }

    data object ErNorskStatsborger: DomeneOpplysning, Effect.Positive {
        override val id = "ER_NORSK_STATSBORGER"
        override val beskrivelse = "Personen start/stopp av periode utføres på er norsk statsborger"
    }

    data object UkjentFoedselsdato : DomeneOpplysning, Effect.Negative {
        override val id = "UKJENT_FOEDSELSDATO"
        override val beskrivelse = "Personen start/stopp av periode utføres på har ukjent fødselsdato"
    }

    data object UkjentFoedselsaar : DomeneOpplysning, Effect.Negative {
        override val id = "UKJENT_FOEDSELSAAR"
        override val beskrivelse = "Personen start/stopp av periode utføres på har ukjent fødselsår"
    }

    data object TokenxPidIkkeFunnet : DomeneOpplysning, Effect.Neutral {
        override val id = "TOKENX_PID_IKKE_FUNNET"
        override val beskrivelse =
            "Innlogget bruker er ikke en logget inn via TOKENX med PID(dvs ikke sluttbruker via ID-Porten)"
    }

    data object OpphoertIdentitet : DomeneOpplysning, Effect.Negative {
        override val id = "OPPHOERT_IDENTITET"
        override val beskrivelse =
            "Personen start/stopp av periode utføres på har opphørt identitet(annulert i Folkeregisteret)"
    }

    data object IkkeBosatt : DomeneOpplysning, Effect.Negative {
        override val id = "IKKE_BOSATT"
        override val beskrivelse =
            "Personen start/stopp av periode utføres på er ikke bosatt i Norge(eventuelt er innlytting anullert)"
    }

    data object ErDoed : DomeneOpplysning, Effect.Negative {
        override val id = "DOED"
        override val beskrivelse = "Personen start/stopp av periode utføres på er død"
    }

    data object ErSavnet : DomeneOpplysning, Effect.Negative {
        override val id = "SAVNET"
        override val beskrivelse = "Personen start/stopp av periode utføres på er savnet"
    }

    data object HarNorskAdresse : DomeneOpplysning, Effect.Positive {
        override val id = "HAR_NORSK_ADRESSE"
        override val beskrivelse = "Personen start/stopp av periode utføres på har norsk adresse"
    }

    data object HarUtenlandskAdresse : DomeneOpplysning, Effect.Negative {
        override val id = "HAR_UTENLANDSK_ADRESSE"
        override val beskrivelse = "Personen start/stopp av periode utføres på har utenlandsk adresse"
    }

    data object HarRegistrertAdresseIEuEoes: DomeneOpplysning, Effect.Positive {
        override val id = "HAR_REGISTRERT_ADRESSE_I_EU_EOES"
        override val beskrivelse = "Personen start/stopp av periode utføres på har en registrert adresse i EØS/EU"
    }

    data object IngenAdresseFunnet : DomeneOpplysning, Effect.Negative {
        override val id = "INGEN_ADRESSE_FUNNET"
        override val beskrivelse = "Personen start/stopp av periode utføres på har ingen adresse i våre systemer"
    }

    data object BosattEtterFregLoven : DomeneOpplysning, Effect.Positive {
        override val id = "BOSATT_ETTER_FREG_LOVEN"
        override val beskrivelse =
            "Personen start/stopp av periode utføres på er bosatt i Norge i henhold til Folkeregisterloven"
    }

    data object Dnummer : DomeneOpplysning, Effect.Positive {
        override val id = "DNUMMER"
        override val beskrivelse = "Personen start/stopp av periode utføres på har D-nummer"
    }

    data object UkjentForenkletFregStatus : DomeneOpplysning, Effect.Negative {
        override val id = "UKJENT_FORENKLET_FREG_STATUS"
        override val beskrivelse = "Personen start/stopp av periode utføres på har ukjent forenklet folkeregisterstatus"
    }

    data object HarGyldigOppholdstillatelse : DomeneOpplysning, Effect.Positive {
        override val id = "HAR_GYLDIG_OPPHOLDSTILLATELSE"
        override val beskrivelse = "Personen start/stopp av periode utføres på har gyldig oppholdstillatelse"
    }

    data object OppholdstillatelseUtgaaatt : DomeneOpplysning, Effect.Negative {
        override val id = "OPPHOLDSTILATELSE_UTGAATT"
        override val beskrivelse = "Personen start/stopp av periode utføres på har oppholdstillatelse som er utgått"
    }

    data object BarnFoedtINorgeUtenOppholdstillatelse : DomeneOpplysning, Effect.Negative {
        override val id = "BARN_FOEDT_I_NORGE_UTEN_OPPHOLDSTILLATELSE"
        override val beskrivelse = "Personen start/stopp av periode utføres på er født i Norge uten oppholdstillatelse"
    }

    data object IngenInformasjonOmOppholdstillatelse : DomeneOpplysning, Effect.Neutral {
        override val id = "INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE"
        override val beskrivelse =
            "Personen start/stopp av periode utføres på har ingen informasjon om oppholdstillatelse"
    }

    data object UkjentStatusForOppholdstillatelse : DomeneOpplysning, Effect.Negative {
        override val id = "UKJENT_STATUS_FOR_OPPHOLDSTILLATELSE"
        override val beskrivelse = "Personen start/stopp av periode utføres på har ukjent status for oppholdstillatelse"
    }

    data object PersonIkkeFunnet : DomeneOpplysning, Effect.Negative {
        override val id = "PERSON_IKKE_FUNNET"
        override val beskrivelse = "Personen start/stopp av periode utføres på er ikke funnet i våre systemer"
    }

    data object SisteFlyttingVarUtAvNorge : DomeneOpplysning, Effect.Negative {
        override val id = "SISTE_FLYTTING_VAR_UT_AV_NORGE"
        override val beskrivelse =
            "Personen start/stopp av periode utføres på har en eller flere flyttinger hvorav den siste var ut av Norge"
    }

    data object SisteFlyttingVarInnTilNorge : DomeneOpplysning, Effect.Positive {
        override val id = "SISTE_FLYTTING_VAR_INN_TIL_NORGE"
        override val beskrivelse =
            "Personen start/stopp av periode utføres på har en eller flere flyttinger hvorav den siste var inn til Norge"
    }

    data object IkkeMuligAAIdentifisereSisteFlytting : DomeneOpplysning, Effect.Neutral {
        override val id = "IKKE_MULIG_AA_IDENTIFISERE_SISTE_FLYTTING"
        override val beskrivelse =
            "Personen start/stopp av periode utføres på har en eller flere flyttinger hvorav den siste ikke er mulig å identifisere"
    }

    data object IngenFlytteInformasjon : DomeneOpplysning, Effect.Neutral {
        override val id = "INGEN_FLYTTE_INFORMASJON"
        override val beskrivelse = "Personen start/stopp av periode utføres på har ingen flytte informasjon"
    }
}