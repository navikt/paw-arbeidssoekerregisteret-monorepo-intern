package no.nav.paw.bqadapter

import java.security.MessageDigest
import java.util.*

class Encoder(
    private val identSalt: ByteArray,
    private val periodeIdSalt: ByteArray,
) {
    private val hexformat = HexFormat.of()
    private val digestAlgorithm = "SHA-256"

    override fun toString(): String {
        return "Encoder(algorithm=${digestAlgorithm}, hexformat=$hexformat, identSalt=${identSalt.size} bytes, periodeIdSalt=${periodeIdSalt.size}) bytes"
    }

    fun encodeIdent(ident: String): String {
        return encode(identSalt, ident)
    }

    fun encodePeriodeId(periodeId: UUID): String {
        return encode(periodeIdSalt, periodeId.toString())
    }

    private fun encode(salt: ByteArray, source: String): String {
        val messageDigest = MessageDigest.getInstance(digestAlgorithm)
        messageDigest.update(salt)
        return hexformat.formatHex(messageDigest.digest(source.toByteArray()))
    }
}

fun <A> ((Encoder, A) -> Map<String, Any>).withEncoder(encoder: Encoder): (A) -> Map<String, Any> =  { a: A -> this(encoder, a) }
