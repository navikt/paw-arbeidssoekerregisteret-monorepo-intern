package no.nav.paw.arbeidssoekerregisteret.model

import no.nav.paw.arbeidssoekerregisteret.config.MinSideVarsel
import no.nav.paw.arbeidssoekerregisteret.config.MinSideVarselConfig
import no.nav.paw.arbeidssoekerregisteret.config.asEksternVarslingBestilling
import no.nav.paw.arbeidssoekerregisteret.config.asSensitivitet
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
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.util.*

class VarselMeldingBygger(
    private val runtimeEnvironment: RuntimeEnvironment,
    private val minSideVarselConfig: MinSideVarselConfig
) {
    fun opprettPeriodeAvsluttetBeskjed(
        varselId: UUID,
        identitetsnummer: String
    ): OpprettBeskjed {
        val minSideVarsel = minSideVarselConfig.periodeAvsluttet
        return opprettBeskjed(
            varselId = varselId,
            identitetsnummer = identitetsnummer,
            sensitivitet = minSideVarsel.sensitivitet.asSensitivitet(),
            link = minSideVarsel.link,
            tekster = minSideVarsel.asTekster(),
            eksternVarsling = minSideVarsel.eksterntVarsel?.asEksternVarslingBestilling()
        )
    }

    fun opprettBekreftelseTilgjengeligOppgave(
        varselId: UUID,
        identitetsnummer: String,
        utsettEksternVarslingTil: Instant
    ): OpprettOppgave {
        val minSideVarsel = minSideVarselConfig.bekreftelseTilgjengelig
        return opprettOppgave(
            varselId = varselId,
            identitetsnummer = identitetsnummer,
            sensitivitet = minSideVarsel.sensitivitet.asSensitivitet(),
            link = minSideVarsel.link,
            tekster = minSideVarsel.asTekster(),
            eksternVarsling = minSideVarsel.eksterntVarsel?.asEksternVarslingBestilling(utsettEksternVarslingTil)
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
            eksternVarsling = minSideVarsel.eksterntVarsel?.asEksternVarslingBestilling()
        )
    }

    fun opprettBeskjed(
        varselId: UUID,
        identitetsnummer: String,
        sensitivitet: Sensitivitet,
        link: URI? = null,
        tekster: List<Tekst>,
        eksternVarsling: EksternVarslingBestilling? = null,
        gjelderTilTidspunkt: Instant? = null
    ): OpprettBeskjed =
        VarselActionBuilder.opprett {
            this.varselId = varselId.toString()
            this.ident = identitetsnummer
            this.sensitivitet = sensitivitet
            this.type = Varseltype.Beskjed
            this.link = link?.toString()
            this.produsent = runtimeEnvironment.asProdusent()
            this.tekster.addAll(tekster)
            this.eksternVarsling = eksternVarsling
            this.aktivFremTil = gjelderTilTidspunkt?.atZone(ZoneId.systemDefault())
        }.let { OpprettBeskjed(varselId, it) }

    fun opprettOppgave(
        varselId: UUID,
        identitetsnummer: String,
        sensitivitet: Sensitivitet,
        link: URI? = null,
        tekster: List<Tekst>,
        eksternVarsling: EksternVarslingBestilling? = null,
        gjelderTilTidspunkt: Instant? = null
    ): OpprettOppgave =
        VarselActionBuilder.opprett {
            this.varselId = varselId.toString()
            this.ident = identitetsnummer
            this.sensitivitet = sensitivitet
            this.type = Varseltype.Oppgave
            this.link = link?.toString()
            this.produsent = runtimeEnvironment.asProdusent()
            this.tekster.addAll(tekster)
            this.eksternVarsling = eksternVarsling
            this.aktivFremTil = gjelderTilTidspunkt?.atZone(ZoneId.systemDefault())
        }.let { OpprettOppgave(varselId, it) }

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
