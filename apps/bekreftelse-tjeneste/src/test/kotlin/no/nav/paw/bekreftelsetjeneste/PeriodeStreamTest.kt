package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.testdata.kafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelse.internehendelser.PeriodeAvsluttet
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.initTilstand
import no.nav.paw.bekreftelsetjeneste.topology.StateStore
import java.time.Instant

class PeriodeStreamTest : FreeSpec({
    val identitetsnummer = "12345678901"
    val startTime = Instant.parse("2024-01-01T08:00:00Z")

    "Tilstand er null og periode er avsluttet - gjør ingenting" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (_, key, periode) = periode(identitetsnummer = identitetsnummer, avsluttetMetadata = metadata(tidspunkt = startTime))
                periodeTopic.pipeInput(key, periode)

                val stateStore: StateStore = testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                stateStore.get(periode.id) shouldBe null

                bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
            }
        }
    }

    "Tilstand er null og periode er ikke avsluttet, opprett tilstand" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (id, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))
                periodeTopic.pipeInput(key, periode)

                val stateStore: StateStore = testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                val currentState = stateStore.get(periode.id)
                currentState.shouldBeInstanceOf<InternTilstand>()
                currentState shouldBe initTilstand(id, key, periode)
            }
        }
    }

    "Tilstand er ikke null og periode er avsluttet, slett state og send PeriodeAvsluttet hendelse" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (_, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))
                periodeTopic.pipeInput(key, periode)
                val stateStore: StateStore = testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)

                stateStore.get(periode.id).shouldBeInstanceOf<InternTilstand>()

                val (_, _, periode2) = periode(periodeId = periode.id, identitetsnummer = identitetsnummer, avsluttetMetadata = metadata(tidspunkt = startTime))
                periodeTopic.pipeInput(key, periode2)

                stateStore.get(periode.id) shouldBe null
                bekreftelseHendelseloggTopicOut.isEmpty shouldBe false
                val kv = bekreftelseHendelseloggTopicOut.readKeyValue()
                kv.key shouldBe key
                kv.value.shouldBeInstanceOf<PeriodeAvsluttet>()
            }
        }
    }

    "Tilstand er ikke null og periode er ikke avsluttet, gjør ingenting" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (id, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))
                val stateStore: StateStore = testDriver.getKeyValueStore(applicationConfig.kafkaTopology.internStateStoreName)
                stateStore.put(periode.id, initTilstand(id, key, periode))
                val state = stateStore.get(periode.id)
                periodeTopic.pipeInput(key, periode)

                stateStore.get(periode.id) shouldBe state
                bekreftelseHendelseloggTopicOut.isEmpty shouldBe true
            }
        }
    }

})