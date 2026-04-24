package no.nav.paw.health.probe

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.apache.kafka.streams.KafkaStreams

class KafkaStreamsHealthProbeTest : FreeSpec({
    "Skal testet helsesjekk for Kafka Streams" {
        val kafkaStreamsMock = mockk<KafkaStreams>()
        val probe = KafkaStreamsHealthProbe(kafkaStreamsMock)
        every { kafkaStreamsMock.state() } returns KafkaStreams.State.CREATED
        probe.isAlive() shouldBe true
        probe.isReady() shouldBe false
        every { kafkaStreamsMock.state() } returns KafkaStreams.State.REBALANCING
        probe.isAlive() shouldBe true
        probe.isReady() shouldBe true
        every { kafkaStreamsMock.state() } returns KafkaStreams.State.RUNNING
        probe.isAlive() shouldBe true
        probe.isReady() shouldBe true
        every { kafkaStreamsMock.state() } returns KafkaStreams.State.PENDING_SHUTDOWN
        probe.isAlive() shouldBe false
        probe.isReady() shouldBe false
        every { kafkaStreamsMock.state() } returns KafkaStreams.State.NOT_RUNNING
        probe.isAlive() shouldBe false
        probe.isReady() shouldBe false
        every { kafkaStreamsMock.state() } returns KafkaStreams.State.PENDING_ERROR
        probe.isAlive() shouldBe false
        probe.isReady() shouldBe false
        every { kafkaStreamsMock.state() } returns KafkaStreams.State.ERROR
        probe.isAlive() shouldBe false
        probe.isReady() shouldBe false
    }
})
