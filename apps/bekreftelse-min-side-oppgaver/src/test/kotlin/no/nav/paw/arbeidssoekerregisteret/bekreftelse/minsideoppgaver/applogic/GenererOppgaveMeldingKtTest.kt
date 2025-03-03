package no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.applogic.varselbygger.VarselMeldingBygger
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.MIN_SIDE_VARSEL_CONFIG
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.config.MinSideVarselConfig
import no.nav.paw.arbeidssoekerregisteret.bekreftelse.minsideoppgaver.vo.InternTilstand
import no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse.bekreftelseTilgjengelig
import no.nav.paw.config.env.Local
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import org.slf4j.LoggerFactory
import java.util.*

private val oppgaveGeneratorLogger = LoggerFactory.getLogger("oppgaveGeneratorLogger")

class GenererOppgaveMeldingKtTest : FreeSpec({
    val varselMeldingBygger = VarselMeldingBygger(
        runtimeEnvironment = Local,
        minSideVarselConfig = loadNaisOrLocalConfiguration<MinSideVarselConfig>(MIN_SIDE_VARSEL_CONFIG)
    )

    "Når en ny bekreftelse blir tilgjengelig skal det genereres en oppgave " {
        val gjeldeneTilstand = InternTilstand(
            periodeId = UUID.randomUUID(), ident = "12345678909", bekreftelser = emptyList()
        )
        val bekreftgelseTilgjengelig = bekreftelseTilgjengelig(
            periodeId = gjeldeneTilstand.periodeId,
        )
        gjeldeneTilstand.asOppgaveMeldinger(
            varselMeldingBygger = varselMeldingBygger,
            hendelse = bekreftgelseTilgjengelig
        ) should { (nyTilstand, oppgaveMeldinger) ->
            runCatching {
                nyTilstand shouldBe InternTilstand(
                    periodeId = gjeldeneTilstand.periodeId,
                    ident = gjeldeneTilstand.ident,
                    bekreftelser = listOf(bekreftgelseTilgjengelig.bekreftelseId)
                )
                oppgaveMeldinger.size shouldBe 1
                oppgaveMeldinger.first().varselId shouldBe bekreftgelseTilgjengelig.bekreftelseId
            }.onFailure { _ ->
                oppgaveGeneratorLogger.info("Ny tilstand: {}", nyTilstand)
                oppgaveGeneratorLogger.info("Oppgave meldinger: {}", oppgaveMeldinger)
            }.getOrThrow()
        }
    }
})
