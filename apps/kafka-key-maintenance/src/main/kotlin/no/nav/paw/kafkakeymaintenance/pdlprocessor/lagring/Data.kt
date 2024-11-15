package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import java.time.Instant

data class Data(
    val id: String,
    val traceparant: ByteArray?,
    val time: Instant,
    val data: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Data

        if (id != other.id) return false
        if (traceparant != null) {
            if (other.traceparant == null) return false
            if (!traceparant.contentEquals(other.traceparant)) return false
        } else if (other.traceparant != null) return false
        if (time != other.time) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (traceparant?.contentHashCode() ?: 0)
        result = 31 * result + time.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }


}