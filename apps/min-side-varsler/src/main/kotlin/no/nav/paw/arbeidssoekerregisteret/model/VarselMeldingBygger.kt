package no.nav.paw.arbeidssoekerregisteret.model

import no.nav.paw.arbeidssoekerregisteret.config.MinSideVarsel
import no.nav.paw.arbeidssoekerregisteret.config.MinSideVarselConfig
import no.nav.paw.arbeidssoekerregisteret.config.asEksternVarslingBestilling
import no.nav.paw.arbeidssoekerregisteret.config.asSensitivitet
import no.nav.paw.arbeidssoekerregisteret.utils.tilNesteFredagKl9
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.clusterNameOrDefaultForLocal
import no.nav.paw.config.env.namespaceOrDefaultForLocal
import no.nav.tms.varsel.action.EksternVarslingBestilling
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import java.time.Instant
import java.time.ZoneId
import java.util.*

class VarselMeldingBygger(
    private val runtimeEnvironment: RuntimeEnvironment,
    private val minSideVarselConfig: MinSideVarselConfig
) {
    fun opprettPeriodeAvsluttetBeskjed(periode: Periode): OpprettBeskjed {
        val minSideVarsel = minSideVarselConfig.periodeAvsluttet
        val eksterntVarsel = periode.avsluttet.utfoertAv.type
            .takeIf { it != BrukerType.SLUTTBRUKER }
            ?.let { minSideVarsel.eksterntVarsel?.asEksternVarslingBestilling() }
        return opprettBeskjed(
            varselId = periode.id,
            identitetsnummer = periode.identitetsnummer,
            sensitivitet = minSideVarsel.sensitivitet.asSensitivitet(),
            link = minSideVarsel.link,
            tekster = minSideVarsel.asTekster(),
            eksterntVarsel = eksterntVarsel
        )
    }

    fun opprettBekreftelseTilgjengeligOppgave(periode: Periode, hendelse: BekreftelseTilgjengelig): OpprettOppgave {
        val minSideVarsel = minSideVarselConfig.bekreftelseTilgjengelig
        val utsettEksternVarslingTil = hendelse.gjelderTil.tilNesteFredagKl9()
        return opprettOppgave(
            varselId = hendelse.bekreftelseId,
            identitetsnummer = periode.identitetsnummer,
            sensitivitet = minSideVarsel.sensitivitet.asSensitivitet(),
            link = minSideVarsel.link,
            tekster = minSideVarsel.asTekster(),
            eksterntVarsel = minSideVarsel.eksterntVarsel?.asEksternVarslingBestilling(utsettEksternVarslingTil)
        )
    }

    fun opprettManueltVarsel(
        varselId: UUID,
        identitetsnummer: String
    ): OpprettBeskjed {
        val minSideVarsel = minSideVarselConfig.manueltVarsel
        return opprettBeskjed(
            varselId = varselId,
            identitetsnummer = identitetsnummer,
            sensitivitet = minSideVarsel.sensitivitet.asSensitivitet(),
            link = minSideVarsel.link,
            tekster = minSideVarsel.asTekster(),
            eksterntVarsel = minSideVarsel.eksterntVarsel?.asEksternVarslingBestilling()
        )
    }

    private fun opprettBeskjed(
        varselId: UUID,
        identitetsnummer: String,
        sensitivitet: Sensitivitet,
        link: String? = null,
        tekster: List<Tekst>,
        eksterntVarsel: EksternVarslingBestilling? = null,
        aktivFremTil: Instant? = null
    ): OpprettBeskjed = opprettVarsel(
        varselId = varselId,
        identitetsnummer = identitetsnummer,
        sensitivitet = sensitivitet,
        type = Varseltype.Beskjed,
        link = link,
        tekster = tekster,
        eksterntVarsel = eksterntVarsel,
        aktivFremTil = aktivFremTil
    ).let { OpprettBeskjed(varselId, it) }

    private fun opprettOppgave(
        varselId: UUID,
        identitetsnummer: String,
        sensitivitet: Sensitivitet,
        link: String? = null,
        tekster: List<Tekst>,
        eksterntVarsel: EksternVarslingBestilling? = null,
        aktivFremTil: Instant? = null
    ): OpprettOppgave = opprettVarsel(
        varselId = varselId,
        identitetsnummer = identitetsnummer,
        sensitivitet = sensitivitet,
        type = Varseltype.Oppgave,
        link = link,
        tekster = tekster,
        eksterntVarsel = eksterntVarsel,
        aktivFremTil = aktivFremTil
    ).let { OpprettOppgave(varselId, it) }

    private fun opprettVarsel(
        varselId: UUID,
        identitetsnummer: String,
        sensitivitet: Sensitivitet,
        type: Varseltype,
        link: String? = null,
        tekster: List<Tekst>,
        eksterntVarsel: EksternVarslingBestilling? = null,
        aktivFremTil: Instant? = null
    ): String = VarselActionBuilder.opprett {
        this.varselId = varselId.toString()
        this.ident = identitetsnummer
        this.sensitivitet = sensitivitet
        this.type = type
        this.link = if (link.isNullOrBlank()) null else link
        this.produsent = runtimeEnvironment.asProdusent()
        this.tekster.addAll(tekster)
        if (eksterntVarsel != null) {
            this.eksternVarsling {
                preferertKanal = eksterntVarsel.prefererteKanaler.first()
                smsVarslingstekst = eksterntVarsel.smsVarslingstekst
                epostVarslingstittel = eksterntVarsel.epostVarslingstittel
                epostVarslingstekst = eksterntVarsel.epostVarslingstekst
                kanBatches = eksterntVarsel.kanBatches
                utsettSendingTil = eksterntVarsel.utsettSendingTil
            }
        }
        this.aktivFremTil = aktivFremTil?.atZone(ZoneId.systemDefault())
    }

    fun avsluttVarsel(varselId: UUID): AvsluttVarsel =
        VarselActionBuilder.inaktiver {
            this.varselId = varselId.toString()
            this.produsent = runtimeEnvironment.asProdusent()
        }.let { AvsluttVarsel(varselId, it) }

    private fun RuntimeEnvironment.asProdusent() = Produsent(
        cluster = clusterNameOrDefaultForLocal(),
        namespace = namespaceOrDefaultForLocal(),
        appnavn = appNameOrDefaultForLocal()
    )

    private fun MinSideVarsel.asTekster(): List<Tekst> {
        return tekster.map {
            Tekst(
                spraakkode = it.spraak.kode,
                tekst = it.tekst,
                default = it.spraak == standardSpraak
            )
        }
    }
}
