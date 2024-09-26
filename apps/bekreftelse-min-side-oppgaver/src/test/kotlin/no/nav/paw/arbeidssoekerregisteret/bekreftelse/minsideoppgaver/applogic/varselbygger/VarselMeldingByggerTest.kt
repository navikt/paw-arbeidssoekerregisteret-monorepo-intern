package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.MinSideTekst
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.MinSideVarselKonfigurasjon
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.Spraakkode
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.bekreftelseTilgjengelig
import no.nav.paw.config.env.Local
import java.net.URI
import java.net.URL
import java.net.URLStreamHandler

class VarselMeldingByggerTest : FreeSpec({
    val config = MinSideVarselKonfigurasjon(
        link = URI.create("http://localhost:8080"),
        standardSpraak = Spraakkode("en"),
        tekster = listOf(
            MinSideTekst(Spraakkode("nb"), "Hei!"),
            MinSideTekst(Spraakkode("en"), "Hello!"),
        )
    )
    val bygger = VarselMeldingBygger(
        minSideVarselKonfigurasjon = config,
        runtimeEnvironment = Local
    )

    "opprettOppgave" {
        val hendelse = bekreftelseTilgjengelig()
        val identitetsnummer = "12345678909"
        val resultat = bygger.opprettOppgave(
            identitetsnummer = identitetsnummer,
            hendelse = hendelse
        )
        resultat.varselId shouldBe hendelse.bekreftelseId
        resultat.value should { json ->
            json shouldContain "\"varselId\":\"${hendelse.bekreftelseId}\""
            json shouldContain "\"ident\":\"$identitetsnummer\""
            json shouldContain "\"sensitivitet\":\"high\""
            json shouldContain "\"type\":\"oppgave\""
            json shouldContain "\"link\":\"${config.link}\""
            json shouldContain config.tekster[0].tekst
            json shouldContain config.tekster[1].tekst
        }
    }

    "avsluttOppgave" {
        val bekreftelse = java.util.UUID.randomUUID()
        val resultat = bygger.avsluttOppgave(bekreftelse)
        resultat.varselId shouldBe bekreftelse
        resultat.value should { json ->
            json shouldContain "\"varselId\":\"$bekreftelse\""
        }
    }
})
