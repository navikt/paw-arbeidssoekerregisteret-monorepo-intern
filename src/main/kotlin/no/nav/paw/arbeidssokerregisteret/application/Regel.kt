package no.nav.paw.arbeidssokerregisteret.application

data class Regel<A: Resultat>(
    /**
     * kode for regelen, kan brukes til å identifisere feilsituasjoner i eksterne API.
     */
    val kode : Int,
    /**
     * Beskrivelse av regelen
     */
    val beskrivelse: String,
    /**
     * Opplysninger som må være tilstede for at regelen skal være sann
     */
    val opplysninger: List<Opplysning>,

    private val vedTreff: (Regel<A>, Iterable<Opplysning>) -> A
) {
    fun vedTreff(opplysning: Iterable<Opplysning>): A = vedTreff(this, opplysning)
}

operator fun <A: Resultat> String.invoke(
    vararg opplysninger: Opplysning,
    kode: Int,
    vedTreff: (Regel<A>, Iterable<Opplysning>) -> A
) = Regel(
    kode = kode,
    beskrivelse = this,
    vedTreff = vedTreff,
    opplysninger = opplysninger.toList()
)

fun <A: Resultat> Regel<A>.evaluer(samletOpplysning: Iterable<Opplysning>): Boolean = opplysninger.all { samletOpplysning.contains(it) }

fun <A: Resultat> List<Regel<out A>>.evaluer(opplysninger: Iterable<Opplysning>): A =
    filter { regel -> regel.evaluer(opplysninger) }
        .map { regel -> regel.vedTreff(opplysninger) }
        .first()
