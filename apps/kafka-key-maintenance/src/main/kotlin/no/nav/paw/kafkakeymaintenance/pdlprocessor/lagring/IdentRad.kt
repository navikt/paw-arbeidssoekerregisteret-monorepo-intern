package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

private const val IDENT_TYPE_FOLKEREGISTERET = 1
private const val IDENT_TYPE_AKTORID = 2
private const val IDENT_TYPE_NPID = 3

@JvmInline
value class IdentType private constructor(val verdi: Int) {
    companion object {
        fun fromType(type: Int): IdentType {
            return when (type) {
                IDENT_TYPE_FOLKEREGISTERET -> FOLKEREGISTERET
                IDENT_TYPE_AKTORID -> AKTORID
                IDENT_TYPE_NPID -> NPID
                else -> throw IllegalArgumentException("Ukjent identtype: $type")
            }
        }

        val FOLKEREGISTERET = IdentType(IDENT_TYPE_FOLKEREGISTERET)
        val AKTORID = IdentType(IDENT_TYPE_AKTORID)
        val NPID = IdentType(IDENT_TYPE_NPID)
    }
}

interface Ident {
    val ident: String
    val identType: IdentType
    val gjeldende: Boolean
}

interface HarPersonId {
    val personId: Long
}

interface IdentMedPersonId: Ident, HarPersonId

private data class IdentRad(
    override val ident: String,
    override val identType: IdentType,
    override val gjeldende: Boolean
): Ident

fun identRad(ident: String, identType: Int, gjeldende: Boolean): Ident {
    return identRad(
        ident = ident,
        identType = IdentType.fromType(identType),
        gjeldende = gjeldende
    )
}

fun identRad(ident: String, identType: IdentType, gjeldende: Boolean): Ident {
    return IdentRad(
        ident = ident,
        identType = identType,
        gjeldende = gjeldende
    )
}

fun identRad(personId: Long, ident: String, identType: Int, gjeldende: Boolean): IdentMedPersonId {
    return identRad(
        personId = personId,
        ident = ident,
        identType = IdentType.fromType(identType),
        gjeldende = gjeldende
    )
}

fun identRad(personId: Long, ident: String, identType: IdentType, gjeldende: Boolean): IdentMedPersonId {
    return IdentRadMedPersonId(
        personId = personId,
        ident = ident,
        identType = identType,
        gjeldende = gjeldende
    )
}

fun Ident.medPersonId(id: Long): IdentMedPersonId {
    return IdentRadMedPersonId(
        personId = id,
        ident = ident,
        identType = identType,
        gjeldende = gjeldende
    )
}

private data class IdentRadMedPersonId(
    override val personId: Long,
    override val ident: String,
    override val identType: IdentType,
    override val gjeldende: Boolean
): IdentMedPersonId
