package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.MinSideVarselConfig
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

    fun opprettOppgave(
        identitetsnummer: String,
        bekreftelseId: UUID,
        gjelderTilTidspunkt: Instant
    ): OpprettOppgave =
        VarselActionBuilder.opprett {
            varselId = bekreftelseId.toString()
            ident = identitetsnummer
            sensitivitet = Sensitivitet.High
            type = Varseltype.Oppgave
            link = minSideVarselConfig.link.toString()
            produsent = runtimeEnvironment.produsent()
            minSideVarselConfig.tekster.map { (spraak, tekst) ->
                Tekst(
                    spraakkode = spraak.kode,
                    tekst = tekst,
                    default = spraak == minSideVarselConfig.standardSpraak
                )
            }.forEach(tekster::add)
            eksternVarsling = EksternVarslingBestilling(
                utsettSendingTil = gjelderTilTidspunkt.atZone(ZoneId.systemDefault()),
                prefererteKanaler = minSideVarselConfig.prefererteKanaler
            )
        }.let { OpprettOppgave(bekreftelseId, it) }

    fun avsluttOppgave(bekreftelseId: UUID): AvsluttOppgave =
        VarselActionBuilder.inaktiver {
            varselId = bekreftelseId.toString()
            produsent = runtimeEnvironment.produsent()
        }.let { AvsluttOppgave(bekreftelseId, it) }
}

private fun RuntimeEnvironment.produsent() = Produsent(
    cluster = clusterNameOrDefaultForLocal(),
    namespace = namespaceOrDefaultForLocal(),
    appnavn = appNameOrDefaultForLocal()
)
