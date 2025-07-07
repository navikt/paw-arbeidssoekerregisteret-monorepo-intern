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

class IdentitetHendelseSerdeTest : FreeSpec({
    val identitetHendelseSerializer = IdentitetHendelseSerializer()
    val identitetHendelseDeserializer = IdentitetHendelseDeserializer()
    val endretHendelse1 = IdentiteterEndretHendelse(
        identiteter = listOf(
            Identitet(identitet = "1", type = IdentitetType.AKTORID, gjeldende = true),
            Identitet(identitet = "2", type = IdentitetType.NPID, gjeldende = true),
            Identitet(identitet = "3", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = false),
            Identitet(identitet = "4", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
            Identitet(identitet = "5", type = IdentitetType.ARBEIDSSOEKERID, gjeldende = true)
        ),
        tidligereIdentiteter = listOf(
            Identitet(identitet = "1", type = IdentitetType.AKTORID, gjeldende = true),
            Identitet(identitet = "2", type = IdentitetType.NPID, gjeldende = true),
            Identitet(identitet = "3", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
            Identitet(identitet = "5", type = IdentitetType.ARBEIDSSOEKERID, gjeldende = true)
        )
    )
    val mergetHendelse1 = IdentiteterMergetHendelse(
        identiteter = listOf(
            Identitet(identitet = "6", type = IdentitetType.AKTORID, gjeldende = true),
            Identitet(identitet = "7", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
            Identitet(identitet = "8", type = IdentitetType.ARBEIDSSOEKERID, gjeldende = false),
            Identitet(identitet = "9", type = IdentitetType.ARBEIDSSOEKERID, gjeldende = true)
        ),
        tidligereIdentiteter = listOf(
            Identitet(identitet = "6", type = IdentitetType.AKTORID, gjeldende = true),
            Identitet(identitet = "7", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
            Identitet(identitet = "8", type = IdentitetType.ARBEIDSSOEKERID, gjeldende = true)
        )
    )

    "Skal serialisere og deserialisere hendelser" {
        val endretHendelseBytes1 = identitetHendelseSerializer.serialize("whatever", endretHendelse1)
        val endretHendelseString1 = identitetHendelseSerializer.serializeToString(endretHendelse1)
        val mergetHendelseBytes1 = identitetHendelseSerializer.serialize("whatever", mergetHendelse1)
        val mergetHendelseString1 = identitetHendelseSerializer.serializeToString(mergetHendelse1)
        val deserEndretHendelse1 = identitetHendelseDeserializer.deserialize("whatever", endretHendelseBytes1)
        val deserEndretHendelse2 = identitetHendelseDeserializer.deserializeFromString(endretHendelseString1)
        val deserMergetHendelse1 = identitetHendelseDeserializer.deserialize("whatever", mergetHendelseBytes1)
        val deserMergetHendelse2 = identitetHendelseDeserializer.deserializeFromString(mergetHendelseString1)

        endretHendelseBytes1 shouldNotBe null
        endretHendelseString1 shouldBe endretHendelseBytes1?.toString(Charsets.UTF_8)
        mergetHendelseBytes1 shouldNotBe null
        mergetHendelseString1 shouldBe mergetHendelseBytes1?.toString(Charsets.UTF_8)
        endretHendelse1 equalTo deserEndretHendelse1
        endretHendelse1 equalTo deserEndretHendelse2
        mergetHendelse1 equalTo deserMergetHendelse1
        mergetHendelse1 equalTo deserMergetHendelse2
    }
})