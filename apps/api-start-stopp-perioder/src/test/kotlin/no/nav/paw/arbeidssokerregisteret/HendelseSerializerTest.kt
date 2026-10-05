package no.nav.paw.arbeidssokerregisteret

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

class HendelseSerializerTest : FreeSpec({
    val json = requireNotNull(javaClass.getResource("/hendelser/startet.json")) {
        "Fant ikke /hendelser/startet.json"
    }.readBytes()
    val treeMapper = JsonMapper.builder().build()

    "Startet-hendelse produsert med Jackson 2 kan leses og skrives uten endret format" {
        val hendelse = HendelseDeserializer().deserialize("topic", json)

        hendelse.shouldBeInstanceOf<Startet>()
        hendelse.metadata.tidspunkt shouldBe Instant.ofEpochSecond(1790690278, 519522700)

        val serialisert = HendelseSerializer().serialize("topic", hendelse)

        treeMapper.readTree(serialisert) shouldBe treeMapper.readTree(json)
    }
})
