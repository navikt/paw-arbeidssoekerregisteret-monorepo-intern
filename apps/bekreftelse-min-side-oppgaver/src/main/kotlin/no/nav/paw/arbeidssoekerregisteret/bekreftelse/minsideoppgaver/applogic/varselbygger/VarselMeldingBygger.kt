package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger

import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.MinSideVarselKonfigurasjon
import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.clusterNameOrDefaultForLocal
import no.nav.paw.config.env.namespaceOrDefaultForLocal
import no.nav.tms.varsel.action.*
import no.nav.tms.varsel.builder.VarselActionBuilder
import java.time.ZoneId
import java.util.*

class VarselMeldingBygger(
    private val minSideVarselKonfigurasjon: MinSideVarselKonfigurasjon,
    private val runtimeEnvironment: RuntimeEnvironment
) {

    fun opprettOppgave(identitetsnummer: String, hendelse: BekreftelseTilgjengelig): OpprettOppgave =
        VarselActionBuilder.opprett {
            varselId = hendelse.bekreftelseId.toString()
            ident = identitetsnummer
            sensitivitet = Sensitivitet.High
            type = Varseltype.Oppgave
            link = minSideVarselKonfigurasjon.link.toString()
            produsent = runtimeEnvironment.produsent()
            minSideVarselKonfigurasjon.tekster.map { (spraak, tekst) ->
                Tekst(
                    spraakkode = spraak.kode,
                    tekst = tekst,
                    default = spraak == minSideVarselKonfigurasjon.standardSpraak
                )
            }.forEach(tekster::add)
            eksternVarsling = EksternVarslingBestilling(
                utsettSendingTil = hendelse.gjelderTil.atZone(ZoneId.systemDefault()),
                prefererteKanaler = listOf(EksternKanal.SMS)
            )
        }.let { OpprettOppgave(hendelse.bekreftelseId, it) }


    fun avsluttOppgave(bekreftelse: UUID): AvsluttOppgave =
        VarselActionBuilder.inaktiver {
            varselId = bekreftelse.toString()
            produsent = runtimeEnvironment.produsent()
        }.let { AvsluttOppgave(bekreftelse, it) }

}

fun RuntimeEnvironment.produsent() = Produsent(
    cluster = clusterNameOrDefaultForLocal(),
    namespace = namespaceOrDefaultForLocal(),
    appnavn = appNameOrDefaultForLocal()
)
