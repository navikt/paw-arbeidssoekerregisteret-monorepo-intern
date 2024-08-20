package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.utils

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.adreseOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.alderOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.euEoesStatsborgerOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.forenkletFregOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.gbrStatsborgerOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.norskStatsborgerOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.oppholdstillatelseOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.utflyttingOpplysning
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import no.nav.paw.pdl.graphql.generated.hentperson.Person

fun Set<Opplysning>.toAarsak(): String =
    this.joinToString(separator = ", ") {
        when (it) {
            Opplysning.DOED -> "Personen er doed"
            Opplysning.SAVNET -> "Personen er savnet"
            Opplysning.IKKE_BOSATT -> "Personen er ikke bosatt etter folkeregisterloven"
            Opplysning.OPPHOERT_IDENTITET -> "Personen har opphoert identitet"
            else -> it.name
        }
    }

fun List<no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning>.toAarsak(): String =
    this.joinToString(separator = ", ") {
        when (it) {
            DomeneOpplysning.ErDoed -> "Personen er død"
            DomeneOpplysning.ErSavnet -> "Personen er savnet"
            DomeneOpplysning.IkkeBosatt -> "Personen er ikke bosatt etter folkeregisterloven"
            DomeneOpplysning.OpphoertIdentitet -> "Personen har opphørt identitet"
            DomeneOpplysning.ErForhaandsgodkjent -> "Registrering er forhåndsgodkjent av NAV-ansatt"
            DomeneOpplysning.ErOver18Aar -> "Personen er over 18 år"
            DomeneOpplysning.ErUnder18Aar -> "Personen er under 18 år"
            DomeneOpplysning.ErEuEoesStatsborger -> "Personen er EØS/EU statsborger"
            DomeneOpplysning.ErGbrStatsborger -> "Personen er britisk statsborger"
            DomeneOpplysning.ErNorskStatsborger -> "Personen er norsk statsborger"
            DomeneOpplysning.UkjentFoedselsdato -> "Personen har ukjent fødselsdato"
            DomeneOpplysning.UkjentFoedselsaar -> "Personen har ukjent fødselsår"
            DomeneOpplysning.TokenxPidIkkeFunnet -> "Innlogget bruker er ikke en logget inn via TOKENX med PID"
            DomeneOpplysning.HarNorskAdresse -> "Personen har norsk adresse"
            DomeneOpplysning.HarUtenlandskAdresse -> "Personen har utenlandsk adresse"
            DomeneOpplysning.HarRegistrertAdresseIEuEoes -> "Personen har en registrert adresse i EØS/EU"
            DomeneOpplysning.IngenAdresseFunnet -> "Personen har ingen adresse i våre systemer"
            DomeneOpplysning.BosattEtterFregLoven -> "Personen er bosatt i Norge i henhold til Folkeregisterloven"
            DomeneOpplysning.Dnummer -> "Personen har D-nummer"
            DomeneOpplysning.UkjentForenkletFregStatus -> "Personen har ukjent forenklet folkeregisterstatus"
            DomeneOpplysning.HarGyldigOppholdstillatelse -> "Personen har gyldig oppholdstillatelse"
            DomeneOpplysning.OppholdstillatelseUtgaaatt -> "Personen har oppholdstillatelse som er utgått"
            DomeneOpplysning.BarnFoedtINorgeUtenOppholdstillatelse -> "Personen er født i Norge uten oppholdstillatelse"
            DomeneOpplysning.IngenInformasjonOmOppholdstillatelse -> "Personen har ingen informasjon om oppholdstillatelse"
            DomeneOpplysning.UkjentStatusForOppholdstillatelse -> "Personen har ukjent status for oppholdstillatelse"
            DomeneOpplysning.PersonIkkeFunnet -> "Personen er ikke funnet i våre systemer"
            DomeneOpplysning.SisteFlyttingVarUtAvNorge -> "Personens siste flytting var ut av Norge"
            DomeneOpplysning.SisteFlyttingVarInnTilNorge -> "Personens siste flytting var inn til Norge"
            DomeneOpplysning.IkkeMuligAAIdentifisereSisteFlytting -> "Personens siste flytting er ikke mulig å identifisere"
            DomeneOpplysning.IngenFlytteInformasjon -> "Personen har ingen flytte informasjon"
            else -> it.id
        }
    }

val negativeOpplysninger = setOf(
    Opplysning.IKKE_BOSATT,
    Opplysning.SAVNET,
    Opplysning.DOED,
    Opplysning.OPPHOERT_IDENTITET,
)

val statusToOpplysningMap = mapOf(
    "ikkeBosatt" to Opplysning.IKKE_BOSATT,
    "forsvunnet" to Opplysning.SAVNET,
    "doedIFolkeregisteret" to Opplysning.DOED,
    "opphoert" to Opplysning.OPPHOERT_IDENTITET,
    "dNummer" to Opplysning.DNUMMER
)

fun genererPersonFakta(person: Person): Set<no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning> {
    require(person.foedsel.size <= 1) { "Personen har flere fødselsdatoer enn forventet" }
    require(person.bostedsadresse.size <= 1) { "Personen har flere bostedsadresser enn forventet" }
    require(person.opphold.size <= 1) { "Personen har flere opphold enn forventet" }

    return alderOpplysning(person.foedsel.firstOrNull()) +
            adreseOpplysning(person.bostedsadresse.firstOrNull()) +
            euEoesStatsborgerOpplysning(person.statsborgerskap) +
            gbrStatsborgerOpplysning(person.statsborgerskap) +
            norskStatsborgerOpplysning(person.statsborgerskap) +
            forenkletFregOpplysning(person.folkeregisterpersonstatus) +
            oppholdstillatelseOpplysning(person.opphold.firstOrNull()) +
            utflyttingOpplysning(person.innflyttingTilNorge, person.utflyttingFraNorge)
}