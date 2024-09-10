package no.nav.paw.meldeplikttjeneste

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.rapportering.internehendelser.LeveringsfristUtloept
import no.nav.paw.rapportering.internehendelser.RapporteringTilgjengelig
import no.nav.paw.rapportering.melding.v1.Melding
import java.time.Duration
import java.time.Instant
import java.util.*

@Ignored("Midlertidig disablet av Thomas")
class IngenAndreTarAnsvarTest: FreeSpec({
    with(ApplicationTestContext()) {
        "Applikasjons test hvor ingen andre tar ansvar" - {
            "Bruker avslutter via rapportering" - {
                val (periode, kafkaKeyResponse) = periode(identitetsnummer = "12345678901")
                periodeTopic.pipeInput(kafkaKeyResponse.key, periode)
                "Nå perioden opprettes skal det ikke skje noe" {
                    hendelseLoggTopic.isEmpty shouldBe true
                }
                "Etter 13 dager skal en rapportering være tilgjengelig" {
                    testDriver.advanceWallClockTime(Duration.ofDays(13))
                    hendelseLoggTopic.isEmpty shouldBe false
                    val kv = hendelseLoggTopic.readKeyValue()
                    kv.key shouldBe kafkaKeyResponse.key
                    with(kv.value.shouldBeInstanceOf<RapporteringTilgjengelig>()) {
                        periodeId shouldBe periode.id
                        identitetsnummer shouldBe periode.identitetsnummer
                        arbeidssoekerId shouldBe kafkaKeyResponse.id
                        gjelderFra shouldBe periode.startet.tidspunkt
                    }
                }
                "Når rapporteringen ikke blir besvart innen fristen sendes det ut en melding" {
                    testDriver.advanceWallClockTime(Duration.ofDays(4))
                    hendelseLoggTopic.isEmpty shouldBe false
                    val kv = hendelseLoggTopic.readKeyValue()
                    kv.key shouldBe kafkaKeyResponse.key
                    with(kv.value.shouldBeInstanceOf<LeveringsfristUtloept>()) {
                        periodeId shouldBe periode.id
                        identitetsnummer shouldBe periode.identitetsnummer
                        arbeidssoekerId shouldBe kafkaKeyResponse.id
                    }
                }
            }
    }
}})

context(ApplicationTestContext)
fun periode(
    id: UUID = UUID.randomUUID(),
    identitetsnummer: String = "12345678901",
    startet: Instant = Instant.now(),
    avsluttet: Instant? = null
) = Periode(
    id,
    identitetsnummer,
    Metadata(
        startet,
        Bruker(BrukerType.SLUTTBRUKER, identitetsnummer),
        "junit",
        "tester",
        null
    ),
    avsluttet?.let { Metadata(
        avsluttet,
        Bruker(BrukerType.SLUTTBRUKER, identitetsnummer),
        "junit",
        "tester",
        null)
    }
) to kafkaKeysService(identitetsnummer)

