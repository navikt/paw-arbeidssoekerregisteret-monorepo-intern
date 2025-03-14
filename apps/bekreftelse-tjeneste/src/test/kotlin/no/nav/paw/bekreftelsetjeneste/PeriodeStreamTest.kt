package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.testdata.kafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelsetjeneste.startdatohaandtering.StatiskMapOddetallPartallMap
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.opprettBekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.topology.BekreftelseTilstandStateStore
import java.time.Instant

class PeriodeStreamTest : FreeSpec({
    val identitetsnummer = "12345678901"
    val startTime = Instant.parse("2024-01-01T08:00:00Z")

    "Tilstand er null og periode er avsluttet - gjør ingenting" {
        with(ApplicationTestContext(
            initialWallClockTime = startTime,
            oddetallPartallMap = StatiskMapOddetallPartallMap(emptySequence())
        )) {
            with(kafkaKeyContext()) {
                val (_, key, periode) = periode(identitetsnummer = identitetsnummer, avsluttetMetadata = metadata(tidspunkt = startTime))
                periodeTopic.pipeInput(key, periode)

                val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore = testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                bekreftelseTilstandStateStore.get(periode.id) shouldBe null

                bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
            }
        }
    }

    "Tilstand er null og periode er ikke avsluttet, opprett tilstand" {
        with(ApplicationTestContext(
            initialWallClockTime = startTime,
            oddetallPartallMap = StatiskMapOddetallPartallMap(emptySequence()))
        ) {
            with(kafkaKeyContext()) {
                val (id, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))
                periodeTopic.pipeInput(key, periode)

                val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore = testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                val currentState = bekreftelseTilstandStateStore.get(periode.id)
                currentState.shouldBeInstanceOf<BekreftelseTilstand>()
                currentState shouldBe opprettBekreftelseTilstand(0, id, key, periode)
            }
        }
    }

    "Tilstand er ikke null og periode er avsluttet, slett state og send PeriodeAvsluttet hendelse" {
        with(ApplicationTestContext(
            initialWallClockTime = startTime,
            oddetallPartallMap = StatiskMapOddetallPartallMap(emptySequence()))
        ) {
            with(kafkaKeyContext()) {
                val (_, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))
                periodeTopic.pipeInput(key, periode)
                val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore = testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)

                bekreftelseTilstandStateStore.get(periode.id).shouldBeInstanceOf<BekreftelseTilstand>()

                val (_, _, periode2) = periode(periodeId = periode.id, identitetsnummer = identitetsnummer, avsluttetMetadata = metadata(tidspunkt = startTime))
                periodeTopic.pipeInput(key, periode2)

                bekreftelseTilstandStateStore.get(periode.id) shouldBe null
                bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                val kv = bekreftelseHendelseloggTopicOut.readKeyValue()
                kv.key shouldBe key
                kv.value.shouldBeInstanceOf<PeriodeAvsluttet>()
            }
        }
    }

    "Tilstand er ikke null og periode er ikke avsluttet, gjør ingenting" {
        with(ApplicationTestContext(
            initialWallClockTime = startTime,
            oddetallPartallMap = StatiskMapOddetallPartallMap(emptySequence()))
        ) {
            with(kafkaKeyContext()) {
                val (id, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))
                val bekreftelseTilstandStateStore: BekreftelseTilstandStateStore = testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                bekreftelseTilstandStateStore.put(periode.id, opprettBekreftelseTilstand(0, id, key, periode))
                val state = bekreftelseTilstandStateStore.get(periode.id)
                periodeTopic.pipeInput(key, periode)

                bekreftelseTilstandStateStore.get(periode.id) shouldBe state
                bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
            }
        }
    }

})