package no.nav.paw.arbeidssokerregisteret.intern.v1

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import java.util.*

data class Avsluttet(
    override val hendelseId: UUID,
    override val id: Long,
    override val identitetsnummer: String,
    override val metadata: Metadata,
    override val opplysninger: Set<Opplysning> = emptySet(),
    val periodeId: UUID? = null,
    val kalkulertAarsak: Aarsak = Aarsak.Udefinert,
    val oppgittAarsak: Aarsak = Aarsak.Udefinert,
): Hendelse, HarOpplysninger {
    override val hendelseType: String = avsluttetHendelseType
}

enum class Aarsak(val beskrivelse: String) {
    IkkeFunnet("Person ikke funnet"),
    Savnet("Er registrert som savnet"),
    Doed("Er registrert som død"),
    Opphoert("Har ugyldig/annullert identitet"),
    Under18Aar("Er under 18 år"),
    IkkeBosattINorgeIHenholdTilFolkeregisterloven("Avvist fordi personen ikke er bosatt i Norge i henhold til folkeregisterloven"),
    UkjentAlder("Kunne ikke fastslå alder"),
    EuEoesStatsborgerMenHarStatusIkkeBosatt("Er EU/EØS statsborger, men har status 'ikke bosatt'"),
    BaOmAaAvsluttePeriode("Bruker ba om å avslutte periode"),
    RegisterGracePeriodeUtloept("Bekreftelse graceperiode utløpt"),
    RegisterGracePeriodeUtloeptEtterEksternInnsamling("Bekreftelse graceperiode utløpt etter ekstern innsamling"),
    TekniskFeilUnderKalkuleringAvAarsak("Teknisk feil under kalkulering av årsak"),
    IngenAarsakFunnet("Ingen årsak funnet"),
    @JsonEnumDefaultValue
    Udefinert("Udefinert");
}
