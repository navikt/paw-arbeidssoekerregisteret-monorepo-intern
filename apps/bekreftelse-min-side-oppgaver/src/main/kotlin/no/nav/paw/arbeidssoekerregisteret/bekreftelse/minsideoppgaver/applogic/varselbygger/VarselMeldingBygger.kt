package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger

import no.nav.paw.bekreftelse.internehendelser.BekreftelseTilgjengelig
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.clusterNameOrDefaultForLocal
import no.nav.paw.config.env.namespaceOrDefaultForLocal
import no.nav.tms.varsel.action.*
import no.nav.tms.varsel.builder.VarselActionBuilder
import java.util.*

class VarselMeldingBygger(private val runtimeEnvironment: RuntimeEnvironment) {

    fun opprettOppgave(identitetsnummer: String, hendelse: BekreftelseTilgjengelig): OpprettOppgave =
        VarselActionBuilder.opprett {
            varselId = hendelse.bekreftelseId.toString()
            ident = identitetsnummer
            sensitivitet = Sensitivitet.High
            type = Varseltype.Oppgave
            link = "https://bakreftelse-arbreg.nav.no/?bekreftelse=${hendelse.bekreftelseId}"
            produsent = runtimeEnvironment.produsent()
            tekster + Tekst(
                spraakkode = "nb",
                tekst = "Du har en ny arbeidssøkerbekreftelse som venter",
                default = true
            )
            eksternVarsling = EksternVarslingBestilling(
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