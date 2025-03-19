package no.nav.paw.arbeidssoekerregisteret.model

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.paw.arbeidssoekerregisteret.config.MIN_SIDE_VARSEL_CONFIG
import no.nav.paw.arbeidssoekerregisteret.config.MinSideVarselConfig
import no.nav.paw.arbeidssoekerregisteret.test.TestData
import no.nav.paw.arbeidssoekerregisteret.test.randomFnr
import no.nav.paw.arbeidssoekerregisteret.test.tid
import no.nav.paw.arbeidssoekerregisteret.utils.tilNesteFredagKl9
import no.nav.paw.config.env.Local
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.tms.varsel.action.EventType
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Varseltype
import java.util.*

class VarselMeldingByggerTest : FreeSpec({
    with(TestData) {
        val minSideVarselConfig = loadNaisOrLocalConfiguration<MinSideVarselConfig>(MIN_SIDE_VARSEL_CONFIG)
        val varselMeldingBygger = VarselMeldingBygger(
            minSideVarselConfig = minSideVarselConfig,
            runtimeEnvironment = Local
        )

        "Skal opprette beskjed for avsluttet periode" {
            with(minSideVarselConfig.periodeAvsluttet) {
                val hendelse = lukketPeriode()
                val identitetsnummer = "12345678909"
                val resultat = varselMeldingBygger.opprettPeriodeAvsluttetBeskjed(
                    varselId = hendelse.id,
                    identitetsnummer = identitetsnummer
                )
                resultat.varselId shouldBe hendelse.id
                resultat.value should { json ->
                    json shouldContain "\"@event_name\":\"${EventType.Opprett.toJson()}\""
                    json shouldContain "\"varselId\":\"${hendelse.id}\""
                    json shouldContain "\"ident\":\"$identitetsnummer\""
                    json shouldContain "\"sensitivitet\":\"${Sensitivitet.Substantial.toJson()}\""
                    json shouldContain "\"type\":\"${Varseltype.Beskjed.toJson()}\""
                    json shouldContain "\"link\":\"${link}\""
                    json shouldContain tekster[0].tekst
                    json shouldContain tekster[1].tekst
                    json shouldContain tekster[2].tekst
                    json shouldContain "\"eksternVarsling\":"
                    json shouldContain eksterntVarsel?.smsTekst!!
                    json shouldContain eksterntVarsel?.epostTittel!!
                    json shouldContain eksterntVarsel?.epostTekst!!
                    json shouldNotContain "\"utsettSendingTil\":"
                }
            }
        }

        "Skal opprette oppgave for bekreftelse tilgjengelig" {
            with(minSideVarselConfig.bekreftelseTilgjengelig) {
                val hendelse = bekreftelseTilgjengelig(
                    gjelderFra = "07.03.2025 12:13".tid,
                    gjelderTil = "21.03.2025 14:15".tid
                )
                val identitetsnummer = randomFnr()
                val resultat = varselMeldingBygger.opprettBekreftelseTilgjengeligOppgave(
                    varselId = hendelse.bekreftelseId,
                    identitetsnummer = identitetsnummer,
                    utsettEksternVarslingTil = hendelse.gjelderTil.tilNesteFredagKl9()
                )
                resultat.varselId shouldBe hendelse.bekreftelseId
                resultat.value should { json ->
                    json shouldContain "\"@event_name\":\"${EventType.Opprett.toJson()}\""
                    json shouldContain "\"varselId\":\"${hendelse.bekreftelseId}\""
                    json shouldContain "\"ident\":\"$identitetsnummer\""
                    json shouldContain "\"sensitivitet\":\"${Sensitivitet.Substantial.toJson()}\""
                    json shouldContain "\"type\":\"${Varseltype.Oppgave.toJson()}\""
                    json shouldContain "\"link\":\"${link}\""
                    json shouldContain tekster[0].tekst
                    json shouldContain tekster[1].tekst
                    json shouldContain tekster[2].tekst
                    json shouldContain "\"eksternVarsling\":"
                    json shouldContain eksterntVarsel?.smsTekst!!
                    json shouldContain eksterntVarsel?.epostTittel!!
                    json shouldContain eksterntVarsel?.epostTekst!!
                    json shouldContain "\"utsettSendingTil\":\"2025-03-28T09:00:00"
                }
            }
        }

        "Skal opprette beskjed for manuelt varsel" {
            with(minSideVarselConfig.manueltVarsel) {
                val varselId = UUID.randomUUID()
                val identitetsnummer = randomFnr()
                val resultat = varselMeldingBygger.opprettManueltVarsel(
                    varselId = varselId,
                    identitetsnummer = identitetsnummer
                )
                resultat.varselId shouldBe varselId
                resultat.value should { json ->
                    json shouldContain "\"@event_name\":\"${EventType.Opprett.toJson()}\""
                    json shouldContain "\"varselId\":\"$varselId\""
                    json shouldContain "\"ident\":\"$identitetsnummer\""
                    json shouldContain "\"sensitivitet\":\"${Sensitivitet.Substantial.toJson()}\""
                    json shouldContain "\"type\":\"${Varseltype.Beskjed.toJson()}\""
                    json shouldContain "\"link\":\"${link}\""
                    json shouldContain tekster[0].tekst
                    json shouldContain tekster[1].tekst
                    json shouldContain tekster[2].tekst
                    json shouldNotContain "\"eksternVarsling\":"
                }
            }
        }

        "Skal opprette avslutt varsel" {
            val varselId = UUID.randomUUID()
            val resultat = varselMeldingBygger.avsluttVarsel(varselId)
            resultat.varselId shouldBe varselId
            resultat.value should { json ->
                json shouldContain "\"@event_name\":\"${EventType.Inaktiver.toJson()}\""
                json shouldContain "\"varselId\":\"$varselId\""
            }
        }
    }
})
