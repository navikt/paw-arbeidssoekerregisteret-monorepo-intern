package no.nav.paw.bekreftelse.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.bekreftelse.context.TestContext
import no.nav.paw.bekreftelse.test.TestData

class KafkaTopologyTest : FreeSpec({
    with(TestContext()) {
        "Skal videresende bekreftelse-meldinger" {
            val value = TestData.bekreftelse1
            bekreftelseSourceTopic.pipeInput(1001L, value)

            bekreftelseTargetTopic.isEmpty shouldBe false
            val keyValueList = bekreftelseTargetTopic.readKeyValuesToList()
            keyValueList.size shouldBe 1
            val keyValue = keyValueList.first()
            keyValue.key shouldBe 1001L
            keyValue.value.id shouldBe value.id
            keyValue.value.periodeId shouldBe value.periodeId
            keyValue.value.bekreftelsesloesning shouldBe value.bekreftelsesloesning
        }

        "Skal videresende på-vegne-av-meldinger" {
            val value = TestData.paaVegneAv1
            bekreftelsePaaVegneAvSourceTopic.pipeInput(1002L, value)

            bekreftelsePaaVegneAvTargetTopic.isEmpty shouldBe false
            val keyValueList = bekreftelsePaaVegneAvTargetTopic.readKeyValuesToList()
            keyValueList.size shouldBe 1
            val keyValue = keyValueList.first()
            keyValue.key shouldBe 1002L
            keyValue.value.periodeId shouldBe value.periodeId
            keyValue.value.bekreftelsesloesning shouldBe value.bekreftelsesloesning
        }
    }
})