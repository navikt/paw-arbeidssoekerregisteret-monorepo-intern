package no.nav.paw.arbeidssoekerregisteret

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.paw.arbeidssoekerregisteret.config.MinSideTekst
import no.nav.paw.arbeidssoekerregisteret.config.MinSideVarselConfig
import no.nav.paw.arbeidssoekerregisteret.config.Spraakkode
import no.nav.paw.arbeidssoekerregisteret.model.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.bekreftelseTilgjengelig
import no.nav.paw.config.env.Local
import no.nav.tms.varsel.action.EksternKanal
import java.net.URI

class VarselMeldingByggerTest : FreeSpec({
    val config = MinSideVarselConfig(
        link = URI.create("http://localhost:8080"),
        standardSpraak = Spraakkode("en"),
        tekster = listOf(
            MinSideTekst(Spraakkode("nb"), "Hei!"),
            MinSideTekst(Spraakkode("en"), "Hello!"),
        ),
        prefererteKanaler = listOf(EksternKanal.SMS)
    )
    val bygger = VarselMeldingBygger(
        minSideVarselConfig = config,
        runtimeEnvironment = Local
    )

    "opprettOppgave" {
        val hendelse = bekreftelseTilgjengelig()
        val identitetsnummer = "12345678909"
        val resultat = bygger.opprettOppgave(
            identitetsnummer = identitetsnummer,
            bekreftelseId = hendelse.bekreftelseId,
            gjelderTilTidspunkt = hendelse.gjelderTil
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
