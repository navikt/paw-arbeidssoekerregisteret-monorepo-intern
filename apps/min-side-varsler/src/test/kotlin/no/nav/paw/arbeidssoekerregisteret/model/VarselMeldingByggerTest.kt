package no.nav.paw.arbeidssoekerregisteret.model

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.paw.arbeidssoekerregisteret.config.MIN_SIDE_VARSEL_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.MinSideVarselConfig
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import no.nav.paw.config.env.Local
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

class VarselMeldingByggerTest : FreeSpec({
    with(TestData) {
        val minSideVarselConfig = loadNaisOrLocalConfiguration<MinSideVarselConfig>(MIN_SIDE_VARSEL_CONFIG)
        val varselMeldingBygger = VarselMeldingBygger(
            minSideVarselConfig = minSideVarselConfig,
            runtimeEnvironment = Local
        )

        "Skal opprette oppgave for avsluttet periode" {
            with(minSideVarselConfig.periodeAvsluttet) {
                val hendelse = lukketPeriode()
                val identitetsnummer = "12345678909"
                val resultat = varselMeldingBygger.opprettPeriodeAvsluttetBeskjed(
                    varselId = hendelse.id,
                    identitetsnummer = identitetsnummer
                )
                resultat.varselId shouldBe hendelse.id
                resultat.value should { json ->
                    json shouldContain "\"varselId\":\"${hendelse.id}\""
                    json shouldContain "\"ident\":\"$identitetsnummer\""
                    json shouldContain "\"sensitivitet\":\"substantial\""
                    json shouldContain "\"type\":\"beskjed\""
                    json shouldContain "\"link\":\"${link}\""
                    json shouldContain tekster[0].tekst
                    json shouldContain tekster[1].tekst
                }
            }
        }

        "Skal opprette oppgave for bekreftelse tilgjengelig" {
            with(minSideVarselConfig.bekreftelseTilgjengelig) {
                val hendelse = bekreftelseTilgjengelig()
                val identitetsnummer = randomFnr()
                val resultat = varselMeldingBygger.opprettBekreftelseTilgjengeligOppgave(
                    varselId = hendelse.bekreftelseId,
                    identitetsnummer = identitetsnummer,
                    gjelderTil = hendelse.gjelderTil
                )
                resultat.varselId shouldBe hendelse.bekreftelseId
                resultat.value should { json ->
                    json shouldContain "\"varselId\":\"${hendelse.bekreftelseId}\""
                    json shouldContain "\"ident\":\"$identitetsnummer\""
                    json shouldContain "\"sensitivitet\":\"substantial\""
                    json shouldContain "\"type\":\"oppgave\""
                    json shouldContain "\"link\":\"${link}\""
                    json shouldContain tekster[0].tekst
                    json shouldContain tekster[1].tekst
                }
            }
        }

        "Skal opprette avslutt varsel" {
            val bekreftelse = java.util.UUID.randomUUID()
            val resultat = varselMeldingBygger.avsluttVarsel(bekreftelse)
            resultat.varselId shouldBe bekreftelse
            resultat.value should { json ->
                json shouldContain "\"varselId\":\"$bekreftelse\""
            }
        }
    }
})
