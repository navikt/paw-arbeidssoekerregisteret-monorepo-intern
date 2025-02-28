package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.spec.style.FreeSpec
import no.nav.paw.arbeidssoekerregisteret.testdata.KafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode

class BekreftelseHappyPathCase : FreeSpec({
    val identietsnummer = "12345678901"
    val periodeStartet = "05.02.2025 15:26".timestamp //Første onsdag i februar 2025
    "Test $periodeStartet" - {
        with(
            setOppTest(
                datoOgKlokkeslettVedStart = periodeStartet,
                bekreftelseIntervall = 14.dager,
                tilgjengeligOffset = 3.dager,
                innleveringsfrist = 7.dager
            )
        ) {
            with(KafkaKeyContext(this.kafkaKeysClient)) {
                val periode = periode(
                    identitetsnummer = identietsnummer,
                    startetMetadata = metadata(tidspunkt = periodeStartet)
                )
                "En arbeidssøkerperiode starter ${tidspunkt()}" {
                    periodeTopic.pipeInput(periode.key, periode.value)
                }
                "[${tidspunkt()}]: Ingen bekreftelser skal være publisert" {
                    ingen_bekreftelser_skal_være_publisert()
                }
                still_klokken_frem_til("20.02.2025 16:00".timestamp)
                "[${tidspunkt()}]: Ingen bekreftelser skal være publisert" {
                    ingen_bekreftelser_skal_være_publisert()
                }
                still_klokken_frem_til("21.02.2025 02:00".timestamp)
                val gjelderTil = "24.02.2025 00:00".timestamp
                "[${tidspunkt()}]: En bekreftelse skal være tilgjengelig, gjelder: fra ${periodeStartet.prettyPrint} til ${gjelderTil.prettyPrint}" {
                    bekreftelse_er_tilgjengelig(
                        fra = periodeStartet,
                        til = gjelderTil
                    )
                }
                "[${tidspunkt()}]: Ingen andre bekreftelser skal være publisert" {
                    ingen_flere_hendelser()
                }
                still_klokken_frem_til("24.02.2025 04:00".timestamp)
                val fristUtloept = "24.02.2025 00:00".timestamp
                "[${tidspunkt()}]: Første frist for levering passerte ${fristUtloept.prettyPrint}" {
                    bekreftelse_venter_på_svar(fristUtloept)
                }
                still_klokken_frem_til("27.02.2025 17:00".timestamp)
                val gjenstaaendeTid = 83.timer
                "[${tidspunkt()}]: Intern versel hendelse skal være publisert, $gjenstaaendeTid" {
                    internt_varsel_hendelse_skal_være_publisert(gjenstaaendeTid)
                }
                "[${tidspunkt()}]: Ingen flere hendelser skal være publisert" {
                    ingen_flere_hendelser()
                }
                val sisteFrist = "03.03.2025 00:00".timestamp
                still_klokken_frem_til("03.03.2025 05:00".timestamp)
                "[${tidspunkt()}]: Siste frist for levering passerte ${sisteFrist.prettyPrint}" {
                    bekreftelse_siste_frist_utloept(sisteFrist)
                }
            }
        }
    }
})

