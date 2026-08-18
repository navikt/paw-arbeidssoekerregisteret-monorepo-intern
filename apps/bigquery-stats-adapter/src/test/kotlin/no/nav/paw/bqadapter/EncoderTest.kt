package no.nav.paw.bqadapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID

class EncoderTest : FreeSpec({
    "encodeOpplysningsId" - {
        val identSalt = "ident-salt".toByteArray()
        val periodeIdSalt = "periode-salt".toByteArray()
        val opplysningsId = UUID.fromString("c52ce702-c12f-49ab-a064-bb504613d680")
        val encoder = Encoder(
            identSalt = identSalt,
            periodeIdSalt = periodeIdSalt
        )

        "should use periode salt followed by ident salt" {
            val expected = sha256(periodeIdSalt + identSalt, opplysningsId.toString())

            encoder.encodeOpplysningsId(opplysningsId) shouldBe expected
        }

        "should be domain separated from bekreftelse id" {
            encoder.encodeOpplysningsId(opplysningsId) shouldNotBe
                    encoder.encodeBekreftelseId(opplysningsId)
        }
    }
})

private fun sha256(salt: ByteArray, source: String): String =
    MessageDigest.getInstance("SHA-256")
        .apply { update(salt) }
        .digest(source.toByteArray())
        .let(HexFormat.of()::formatHex)
