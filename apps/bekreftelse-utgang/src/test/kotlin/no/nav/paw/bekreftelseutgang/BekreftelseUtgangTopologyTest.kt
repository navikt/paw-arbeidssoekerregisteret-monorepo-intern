package no.nav.paw.bekreftelseutgang

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.testdata.kafkaKeyContext
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.metadata
import no.nav.paw.arbeidssoekerregisteret.testdata.mainavro.periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.bekreftelse.internehendelser.RegisterGracePeriodeUtloept
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
})

fun graceperiodeUtloeptHendelse(periodeId: UUID, arbeidssoekerId: Long) = RegisterGracePeriodeUtloept(
    hendelseId = UUID.randomUUID(),
    periodeId = periodeId,
    arbeidssoekerId = arbeidssoekerId,
    hendelseTidspunkt = Instant.now(),
    bekreftelseId = UUID.randomUUID(),
)
