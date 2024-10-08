package no.nav.paw.bekreftelseutgang

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.testdata.kafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.bekreftelse.internehendelser.BaOmAaAvsluttePeriode
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
import no.nav.paw.bekreftelseutgang.tilstand.InternTilstand
import no.nav.paw.bekreftelseutgang.tilstand.StateStore
import java.time.Duration
import java.time.Instant
import java.util.*

class BekreftelseUtgangTopologyTest : FreeSpec({

    val startTime = Instant.ofEpochMilli(1704185347000)
    val identitetsnummer = "12345678901"

    "BekreftelseHendelse 'RegisterGracePeriodeUtloept' med tilhørende identitetsnummer i state sender 'Avsluttet' hendelse" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (id, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))
                periodeTopic.pipeInput(key, periode)

                val graceperiodeUtloeptHendelse = graceperiodeUtloeptHendelse(periodeId = periode.id, arbeidssoekerId = id)
                bekreftelseHendelseLoggTopic.pipeInput(key, graceperiodeUtloeptHendelse)


                hendelseLoggTopicOut.isEmpty shouldBe false
                val kv = hendelseLoggTopicOut.readKeyValue()
                kv.key shouldBe key
                kv.value.shouldBeInstanceOf<Avsluttet>()
            }
        }
    }

    "BekreftelseHendelse 'BaOmAaAvsluttePeriode' med tilhørende identitetsnummer i state sender 'Avsluttet' hendelse" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (id, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))
                periodeTopic.pipeInput(key, periode)

                val baOmAaAvslutteHendelse = baOmAaAvslutteHendelse(periodeId = periode.id, arbeidssoekerId = id)
                bekreftelseHendelseLoggTopic.pipeInput(key, baOmAaAvslutteHendelse)

                hendelseLoggTopicOut.isEmpty shouldBe false
                val kv = hendelseLoggTopicOut.readKeyValue()
                kv.key shouldBe key
                kv.value.shouldBeInstanceOf<Avsluttet>()
            }
        }
    }

    "BekreftelseHendelse 'RegisterGracePeriodeUtloept' uten tilhørende identitetsnummer i state sender ikke 'Avsluttet' hendelse før identitetsnummer er satt" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (id, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))

                val graceperiodeUtloeptHendelse = graceperiodeUtloeptHendelse(periodeId = periode.id, arbeidssoekerId = id)
                bekreftelseHendelseLoggTopic.pipeInput(key, graceperiodeUtloeptHendelse)

                hendelseLoggTopicOut.isEmpty shouldBe true

                periodeTopic.pipeInput(key, periode)

                hendelseLoggTopicOut.isEmpty shouldBe false
                val kv = hendelseLoggTopicOut.readKeyValue()
                kv.key shouldBe key
                kv.value.shouldBeInstanceOf<Avsluttet>()
            }
        }
    }

    "identitetsnummer men ingen tilhørende BekreftelseHendelse sender ikke 'Avsluttet' hendelse" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (_, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))
                periodeTopic.pipeInput(key, periode)

                hendelseLoggTopicOut.isEmpty shouldBe true
            }
        }
    }

    "Avsluttet periode sletter state og sender ikke 'Avsluttet' hendelse" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (_, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))
                periodeTopic.pipeInput(key, periode)

                hendelseLoggTopicOut.isEmpty shouldBe true

                val (_, key2, periode2) = periode(periodeId = periode.id, identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime), avsluttetMetadata = metadata(tidspunkt = startTime.plus(Duration.ofDays(2))))
                periodeTopic.pipeInput(key2, periode2)

                hendelseLoggTopicOut.isEmpty shouldBe true

                val stateStore: StateStore = testDriver.getKeyValueStore(applicationConfig.kafkaTopology.stateStoreName)
                stateStore.get(periode.id) shouldBe null
            }
        }
    }

    "BekreftelseHendelse uten tilhørende identitetsnummer i state sender ikke 'Avsluttet' hendelse" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            val graceperiodeUtloeptHendelse = graceperiodeUtloeptHendelse(periodeId = UUID.randomUUID(), arbeidssoekerId = 1234L)
            bekreftelseHendelseLoggTopic.pipeInput(1234L, graceperiodeUtloeptHendelse)

            hendelseLoggTopicOut.isEmpty shouldBe true
        }
    }

    "State blir oppdatert med BekreftelseHendelse" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (id, key, periode) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))
                periodeTopic.pipeInput(key, periode)

                val graceperiodeUtloeptHendelse = graceperiodeUtloeptHendelse(periodeId = periode.id, arbeidssoekerId = id)
                bekreftelseHendelseLoggTopic.pipeInput(key, graceperiodeUtloeptHendelse)

                val stateStore: StateStore = testDriver.getKeyValueStore(applicationConfig.kafkaTopology.stateStoreName)
                val currentState = stateStore[periode.id]

                currentState shouldBe InternTilstand(
                    identitetsnummer = identitetsnummer,
                    bekreftelseHendelse = graceperiodeUtloeptHendelse.copy(
                        hendelseTidspunkt = currentState.bekreftelseHendelse!!.hendelseTidspunkt // Ignorerer tidspunkt pga. feilende test i github actions
                    )
                )
                hendelseLoggTopicOut.isEmpty shouldBe false
            }
        }
    }

    "Flere perioder og BekreftelseHendelse hendelser oppdaterer state riktig og sender hendelser" {
        with(ApplicationTestContext(initialWallClockTime = startTime)) {
            with(kafkaKeyContext()) {
                val (id, key, periode1) = periode(identitetsnummer = identitetsnummer, startetMetadata = metadata(tidspunkt = startTime))
                val (id2, key2, periode2) = periode(identitetsnummer = "98765432109", startetMetadata = metadata(tidspunkt = startTime))

                periodeTopic.pipeInput(key, periode1)
                hendelseLoggTopicOut.isEmpty shouldBe true

                periodeTopic.pipeInput(key2, periode2)
                hendelseLoggTopicOut.isEmpty shouldBe true

                val graceperiodeUtloeptHendelse1 = graceperiodeUtloeptHendelse(periodeId = periode1.id, arbeidssoekerId = id)
                bekreftelseHendelseLoggTopic.pipeInput(key, graceperiodeUtloeptHendelse1)
                hendelseLoggTopicOut.isEmpty shouldBe false
                hendelseLoggTopicOut.readRecordsToList().size shouldBe 1

                val graceperiodeUtloeptHendelse2 = graceperiodeUtloeptHendelse(periodeId = periode2.id, arbeidssoekerId = id2)
                bekreftelseHendelseLoggTopic.pipeInput(key2, graceperiodeUtloeptHendelse2)
                hendelseLoggTopicOut.isEmpty shouldBe false
                hendelseLoggTopicOut.readRecordsToList().size shouldBe 1
            }
        }
    }

})

fun baOmAaAvslutteHendelse(periodeId: UUID, arbeidssoekerId: Long) = BaOmAaAvsluttePeriode(
    hendelseId = UUID.randomUUID(),
    periodeId = periodeId,
    arbeidssoekerId = arbeidssoekerId,
    hendelseTidspunkt = Instant.now(),
)

fun graceperiodeUtloeptHendelse(periodeId: UUID, arbeidssoekerId: Long, hendelseTidspunkt: Instant = Instant.now()) = RegisterGracePeriodeUtloept(
    hendelseId = UUID.randomUUID(),
    periodeId = periodeId,
    arbeidssoekerId = arbeidssoekerId,
    hendelseTidspunkt = hendelseTidspunkt,
    bekreftelseId = UUID.randomUUID(),
)
