package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

fun <A: Ident, B: Ident> diffAtilB(
    a: List<A>,
    b: List<B>
): DiffResultat<A, B> {
    val slettet = a.filter { identA -> identA notInCollection  b}
    val opprettet = b.filter { identB -> identB notInCollection  a}
    return DiffResultat(slettet, opprettet)
}

infix fun Ident.notInCollection(collection: Collection<Ident>): Boolean {
    return collection.none { it.ident == this.ident && it.identType == this.identType && it.gjeldende == this.gjeldende }
}

class DiffResultat<A: Ident, B: Ident>(
    val slettet: List<A>,
    val opprettet: List<B>
)