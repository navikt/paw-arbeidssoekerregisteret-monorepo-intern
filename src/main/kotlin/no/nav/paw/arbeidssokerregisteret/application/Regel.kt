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
     * Fakta som må være tilstede for at regelen skal være sann
     */
    val fakta: List<Fakta>,

    private val vedTreff: (Regel<A>, Iterable<Fakta>) -> A
) {
    fun vedTreff(fakta: Iterable<Fakta>): A = vedTreff(this, fakta)
}

operator fun <A: Resultat> String.invoke(
    vararg fakta: Fakta,
    kode: Int,
    vedTreff: (Regel<A>, Iterable<Fakta>) -> A
) = Regel(
    kode = kode,
    beskrivelse = this,
    vedTreff = vedTreff,
    fakta = fakta.toList()
)

fun <A: Resultat> Regel<A>.evaluer(samletFakta: Iterable<Fakta>): Boolean = fakta.all { samletFakta.contains(it) }

fun <A: Resultat> List<Regel<out A>>.evaluer(samletFakta: Iterable<Fakta>): A =
    filter { regel -> regel.evaluer(samletFakta) }
        .map { regel -> regel.vedTreff(samletFakta) }
        .first()
