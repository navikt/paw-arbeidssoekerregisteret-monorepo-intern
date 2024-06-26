package no.nav.paw.arbeidssokerregisteret.application.opplysninger

interface Opplysning {
    val id: String
    val beskrivelse: String
}


infix operator fun Opplysning.plus(opplysning: Opplysning): Set<Opplysning> = setOf(this, opplysning)
sealed interface DomeneOpplysning: Opplysning {

    data object ErForhaandsgodkjent : DomeneOpplysning {
        override val id = "FORHAANDSGODKJENT_AV_ANSATT"
        override val beskrivelse = "Registrering er forhåndsgodkjent av NAV-ansatt"
    }

    data object ErOver18Aar : DomeneOpplysning {
        override val id = "ER_OVER_18_AAR"
        override val beskrivelse = "Personen start/stopp av periode utføres på er over 18 år"
    }

    data object ErUnder18Aar : DomeneOpplysning {
        override val id = "ER_UNDER_18_AAR"
        override val beskrivelse = "Personen start/stopp av periode utføres på er under 18 år"
    }

    data object ErEuEoesStatsborger : DomeneOpplysning {
        override val id = "ER_EU_EOES_STATSBORGER"
        override val beskrivelse = "Personen start/stopp av periode utføres på er EØS/EU statsborger"
    }

    data object ErGbrStatsborger : DomeneOpplysning {
        override val id = "ER_GBR_STATSBORGER"
        override val beskrivelse = "Personen start/stopp av periode utføres på er britisk statsborger"
    }

    data object UkjentFoedselsdato : DomeneOpplysning {
        override val id = "UKJENT_FOEDSELSDATO"
        override val beskrivelse = "Personen start/stopp av periode utføres på har ukjent fødselsdato"
    }

    data object UkjentFoedselsaar : DomeneOpplysning {
        override val id = "UKJENT_FOEDSELSAAR"
        override val beskrivelse = "Personen start/stopp av periode utføres på har ukjent fødselsår"
    }

    data object TokenxPidIkkeFunnet : DomeneOpplysning {
        override val id = "TOKENX_PID_IKKE_FUNNET"
        override val beskrivelse =
            "Innlogget bruker er ikke en logget inn via TOKENX med PID(dvs ikke sluttbruker via ID-Porten)"
    }

    data object OpphoertIdentitet : DomeneOpplysning {
        override val id = "OPPHOERT_IDENTITET"
        override val beskrivelse =
            "Personen start/stopp av periode utføres på har opphørt identitet(annulert i Folkeregisteret)"
    }

    data object IkkeBosatt : DomeneOpplysning {
        override val id = "IKKE_BOSATT"
        override val beskrivelse =
            "Personen start/stopp av periode utføres på er ikke bosatt i Norge(eventuelt er innlytting anullert)"
    }

    data object ErDoed : DomeneOpplysning {
        override val id = "DOED"
        override val beskrivelse = "Personen start/stopp av periode utføres på er død"
    }

    data object ErSavnet : DomeneOpplysning {
        override val id = "SAVNET"
        override val beskrivelse = "Personen start/stopp av periode utføres på er savnet"
    }

    data object HarNorskAdresse : DomeneOpplysning {
        override val id = "HAR_NORSK_ADRESSE"
        override val beskrivelse = "Personen start/stopp av periode utføres på har norsk adresse"
    }

    data object HarUtenlandskAdresse : DomeneOpplysning {
        override val id = "HAR_UTENLANDSK_ADRESSE"
        override val beskrivelse = "Personen start/stopp av periode utføres på har utenlandsk adresse"
    }

    data object IngenAdresseFunnet : DomeneOpplysning {
        override val id = "INGEN_ADRESSE_FUNNET"
        override val beskrivelse = "Personen start/stopp av periode utføres på har ingen adresse i våre systemer"
    }

    data object BosattEtterFregLoven : DomeneOpplysning {
        override val id = "BOSATT_ETTER_FREG_LOVEN"
        override val beskrivelse =
            "Personen start/stopp av periode utføres på er bosatt i Norge i henhold til Folkeregisterloven"
    }

    data object Dnummer : DomeneOpplysning {
        override val id = "DNUMMER"
        override val beskrivelse = "Personen start/stopp av periode utføres på har D-nummer"
    }

    data object UkjentForenkletFregStatus : DomeneOpplysning {
        override val id = "UKJENT_FORENKLET_FREG_STATUS"
        override val beskrivelse = "Personen start/stopp av periode utføres på har ukjent forenklet folkeregisterstatus"
    }

    data object HarGyldigOppholdstillatelse : DomeneOpplysning {
        override val id = "HAR_GYLDIG_OPPHOLDSTILLATELSE"
        override val beskrivelse = "Personen start/stopp av periode utføres på har gyldig oppholdstillatelse"
    }

    data object OppholdstillatelseUtgaaatt : DomeneOpplysning {
        override val id = "OPPHOLDSTILATELSE_UTGAATT"
        override val beskrivelse = "Personen start/stopp av periode utføres på har oppholdstillatelse som er utgått"
    }

    data object BarnFoedtINorgeUtenOppholdstillatelse : DomeneOpplysning {
        override val id = "BARN_FOEDT_I_NORGE_UTEN_OPPHOLDSTILLATELSE"
        override val beskrivelse = "Personen start/stopp av periode utføres på er født i Norge uten oppholdstillatelse"
    }

    data object IngenInformasjonOmOppholdstillatelse : DomeneOpplysning {
        override val id = "INGEN_INFORMASJON_OM_OPPHOLDSTILLATELSE"
        override val beskrivelse =
            "Personen start/stopp av periode utføres på har ingen informasjon om oppholdstillatelse"
    }

    data object UkjentStatusForOppholdstillatelse : DomeneOpplysning {
        override val id = "UKJENT_STATUS_FOR_OPPHOLDSTILLATELSE"
        override val beskrivelse = "Personen start/stopp av periode utføres på har ukjent status for oppholdstillatelse"
    }

    data object PersonIkkeFunnet : DomeneOpplysning {
        override val id = "PERSON_IKKE_FUNNET"
        override val beskrivelse = "Personen start/stopp av periode utføres på er ikke funnet i våre systemer"
    }

    data object SisteFlyttingVarUtAvNorge : DomeneOpplysning {
        override val id = "SISTE_FLYTTING_VAR_UT_AV_NORGE"
        override val beskrivelse =
            "Personen start/stopp av periode utføres på har en eller flere flyttinger hvorav den siste var ut av Norge"
    }

    data object SisteFlyttingVarInnTilNorge : DomeneOpplysning {
        override val id = "SISTE_FLYTTING_VAR_INN_TIL_NORGE"
        override val beskrivelse =
            "Personen start/stopp av periode utføres på har en eller flere flyttinger hvorav den siste var inn til Norge"
    }

    data object IkkeMuligAAIdentifisereSisteFlytting : DomeneOpplysning {
        override val id = "IKKE_MULIG_AA_IDENTIFISERE_SISTE_FLYTTING"
        override val beskrivelse =
            "Personen start/stopp av periode utføres på har en eller flere flyttinger hvorav den siste ikke er mulig å identifisere"
    }

    data object IngenFlytteInformasjon : DomeneOpplysning {
        override val id = "INGEN_FLYTTE_INFORMASJON"
        override val beskrivelse = "Personen start/stopp av periode utføres på har ingen flytte informasjon"
    }
}