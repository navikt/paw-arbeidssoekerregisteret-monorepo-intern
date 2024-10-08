package no.nav.paw.bekreftelsetjeneste

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.testdata.kafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.initTilstand
import no.nav.paw.bekreftelsetjeneste.topology.StateStore
import java.time.Instant

class PeriodeStreamTest : FreeSpec({
    val identitetsnummer = "12345678901"
    val startTime = Instant.ofEpochMilli(1704185347000)

    "state is null and periode is avsluttet, do nothing" {
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

    "state is null and periode is not avsluttet, update state" {
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

    "state is not null and periode is avsluttet, delete state and emit" {
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
            }
        }
    }

    "state is not null and periode is not avsluttet, do nothing" {
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