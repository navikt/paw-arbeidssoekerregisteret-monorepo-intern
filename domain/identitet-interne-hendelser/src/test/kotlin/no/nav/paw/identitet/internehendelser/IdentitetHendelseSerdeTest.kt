package no.nav.paw.identitet.internehendelser

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import java.time.temporal.ChronoUnit

private infix fun IdentiteterEndretHendelse.equalTo(other: IdentitetHendelse) {
    val that = other.shouldBeInstanceOf<IdentiteterEndretHendelse>()
    hendelseId shouldBe that.hendelseId
    hendelseType shouldBe that.hendelseType
    hendelseTidspunkt.truncatedTo(ChronoUnit.MILLIS) shouldBe that.hendelseTidspunkt.truncatedTo(ChronoUnit.MILLIS)
    identiteter shouldBe that.identiteter
    tidligereIdentiteter shouldBe that.tidligereIdentiteter
}

private infix fun IdentiteterMergetHendelse.equalTo(other: IdentitetHendelse) {
    val that = other.shouldBeInstanceOf<IdentiteterMergetHendelse>()
    hendelseId shouldBe that.hendelseId
    hendelseType shouldBe that.hendelseType
    hendelseTidspunkt.truncatedTo(ChronoUnit.MILLIS) shouldBe that.hendelseTidspunkt.truncatedTo(ChronoUnit.MILLIS)
    identiteter shouldBe that.identiteter
    tidligereIdentiteter shouldBe that.tidligereIdentiteter
}

private infix fun IdentiteterSplittetHendelse.equalTo(other: IdentitetHendelse) {
    val that = other.shouldBeInstanceOf<IdentiteterSplittetHendelse>()
    hendelseId shouldBe that.hendelseId
    hendelseType shouldBe that.hendelseType
    hendelseTidspunkt.truncatedTo(ChronoUnit.MILLIS) shouldBe that.hendelseTidspunkt.truncatedTo(ChronoUnit.MILLIS)
    identiteter shouldBe that.identiteter
    tidligereIdentiteter shouldBe that.tidligereIdentiteter
}

private infix fun IdentiteterSlettetHendelse.equalTo(other: IdentitetHendelse) {
    val that = other.shouldBeInstanceOf<IdentiteterSlettetHendelse>()
    hendelseId shouldBe that.hendelseId
    hendelseType shouldBe that.hendelseType
    hendelseTidspunkt.truncatedTo(ChronoUnit.MILLIS) shouldBe that.hendelseTidspunkt.truncatedTo(ChronoUnit.MILLIS)
    tidligereIdentiteter shouldBe that.tidligereIdentiteter
}

class IdentitetHendelseSerdeTest : FreeSpec({
    val identitetHendelseSerializer = IdentitetHendelseSerializer()
    val identitetHendelseDeserializer = IdentitetHendelseDeserializer()
    val endretHendelse1 = IdentiteterEndretHendelse(
        identiteter = listOf(
            Identitet(identitet = "10001", type = IdentitetType.AKTORID, gjeldende = true),
            Identitet(identitet = "10002", type = IdentitetType.NPID, gjeldende = true),
            Identitet(identitet = "01017100001", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = false),
            Identitet(identitet = "02017100002", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
            Identitet(identitet = "10003", type = IdentitetType.ARBEIDSSOEKERID, gjeldende = true)
        ),
        tidligereIdentiteter = listOf(
            Identitet(identitet = "10001", type = IdentitetType.AKTORID, gjeldende = true),
            Identitet(identitet = "10002", type = IdentitetType.NPID, gjeldende = true),
            Identitet(identitet = "01017100001", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
            Identitet(identitet = "10003", type = IdentitetType.ARBEIDSSOEKERID, gjeldende = true)
        )
    )
    val mergetHendelse1 = IdentiteterMergetHendelse(
        identiteter = listOf(
            Identitet(identitet = "20001", type = IdentitetType.AKTORID, gjeldende = true),
            Identitet(identitet = "01017200001", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = false),
            Identitet(identitet = "01017200002", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
            Identitet(identitet = "20002", type = IdentitetType.ARBEIDSSOEKERID, gjeldende = false),
            Identitet(identitet = "20003", type = IdentitetType.ARBEIDSSOEKERID, gjeldende = true)
        ),
        tidligereIdentiteter = listOf(
            Identitet(identitet = "20001", type = IdentitetType.AKTORID, gjeldende = true),
            Identitet(identitet = "01017200001", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
            Identitet(identitet = "20002", type = IdentitetType.ARBEIDSSOEKERID, gjeldende = true)
        )
    )
    val splittetHendelse1 = IdentiteterSplittetHendelse(
        identiteter = listOf(
            Identitet(identitet = "30002", type = IdentitetType.AKTORID, gjeldende = true),
            Identitet(identitet = "01017300001", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
            Identitet(identitet = "30003", type = IdentitetType.ARBEIDSSOEKERID, gjeldende = true)
        ),
        tidligereIdentiteter = listOf(
            Identitet(identitet = "30001", type = IdentitetType.AKTORID, gjeldende = true),
            Identitet(identitet = "01017300001", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = false),
            Identitet(identitet = "01017300002", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
            Identitet(identitet = "30002", type = IdentitetType.ARBEIDSSOEKERID, gjeldende = true)
        )
    )
    val slettetHendelse1 = IdentiteterSlettetHendelse(
        tidligereIdentiteter = listOf(
            Identitet(identitet = "40001", type = IdentitetType.AKTORID, gjeldende = true),
            Identitet(identitet = "01017400001", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
            Identitet(identitet = "40002", type = IdentitetType.ARBEIDSSOEKERID, gjeldende = true)
        )
    )

    "Skal serialisere og deserialisere hendelser" {
        val endretHendelseBytes1 = identitetHendelseSerializer.serialize("whatever", endretHendelse1)
        val endretHendelseString1 = identitetHendelseSerializer.serializeToString(endretHendelse1)
        val mergetHendelseBytes1 = identitetHendelseSerializer.serialize("whatever", mergetHendelse1)
        val mergetHendelseString1 = identitetHendelseSerializer.serializeToString(mergetHendelse1)
        val splittetHendelseBytes1 = identitetHendelseSerializer.serialize("whatever", splittetHendelse1)
        val splittetHendelseString1 = identitetHendelseSerializer.serializeToString(splittetHendelse1)
        val slettetHendelseBytes1 = identitetHendelseSerializer.serialize("whatever", slettetHendelse1)
        val slettetHendelseString1 = identitetHendelseSerializer.serializeToString(slettetHendelse1)
        val endretHendelseDeser1 = identitetHendelseDeserializer.deserialize("whatever", endretHendelseBytes1)
        val endretHendelseDeser2 = identitetHendelseDeserializer.deserializeFromString(endretHendelseString1)
        val mergetHendelseDeser1 = identitetHendelseDeserializer.deserialize("whatever", mergetHendelseBytes1)
        val mergetHendelseDeser2 = identitetHendelseDeserializer.deserializeFromString(mergetHendelseString1)
        val splittetHendelseDeser1 = identitetHendelseDeserializer.deserialize("whatever", splittetHendelseBytes1)
        val splittetHendelseDeser2 = identitetHendelseDeserializer.deserializeFromString(splittetHendelseString1)
        val slettetHendelseDeser1 = identitetHendelseDeserializer.deserialize("whatever", slettetHendelseBytes1)
        val slettetHendelseDeser2 = identitetHendelseDeserializer.deserializeFromString(slettetHendelseString1)

        endretHendelseBytes1 shouldNotBe null
        endretHendelseString1 shouldBe endretHendelseBytes1?.toString(Charsets.UTF_8)
        mergetHendelseBytes1 shouldNotBe null
        mergetHendelseString1 shouldBe mergetHendelseBytes1?.toString(Charsets.UTF_8)
        splittetHendelseBytes1 shouldNotBe null
        splittetHendelseString1 shouldBe splittetHendelseBytes1?.toString(Charsets.UTF_8)
        slettetHendelseBytes1 shouldNotBe null
        slettetHendelseString1 shouldBe slettetHendelseBytes1?.toString(Charsets.UTF_8)
        endretHendelse1 equalTo endretHendelseDeser1
        endretHendelse1 equalTo endretHendelseDeser2
        mergetHendelse1 equalTo mergetHendelseDeser1
        mergetHendelse1 equalTo mergetHendelseDeser2
        splittetHendelse1 equalTo splittetHendelseDeser1
        splittetHendelse1 equalTo splittetHendelseDeser2
        slettetHendelse1 equalTo slettetHendelseDeser1
        slettetHendelse1 equalTo slettetHendelseDeser2
    }
})