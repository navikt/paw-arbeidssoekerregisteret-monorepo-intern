package no.nav.paw.collections

/***
 * Veldig enkel NonEmptyList implementasjon
 * Arrow sin NonEmptyList skapte litt for ofte krøll sammen med Jackson, samtidig
 * som at den implementerte List noe som gjorde at man ikke alltid hadde helt kontroll på når den ble deserialisert.
 */
interface PawNonEmptyList<A> {
    val first: A
    val tail: List<A>

    fun <R> map(f: (A) -> R): PawNonEmptyList<R>
    fun <R> flatMap(f: (A) -> PawNonEmptyList<R>): PawNonEmptyList<R>

    fun <T: Comparable<T>> maxBy(f: (A) -> T): A = toList().maxBy(f)

    infix operator fun plus(other: List<A>): PawNonEmptyList<A>
    infix operator fun plus(other: A): PawNonEmptyList<A>

    fun toList(): List<A>
    fun toSet(): Set<A>

    val size: Int get() = 1 + tail.size
}

fun <A: Comparable<A>> PawNonEmptyList<A>.max(): A = toList().max()

fun <A> pawNonEmptyListOf(head: A, tail: List<A>): PawNonEmptyList<A> = PawNonEmptyListImpl(head, tail)
fun <A> pawNonEmptyListOf(head: A): PawNonEmptyList<A> = PawNonEmptyListImpl(head, emptyList())

fun <A> Collection<A>.toPawNonEmptyListOrNull(): PawNonEmptyList<A>? = if (isEmpty()) null else pawNonEmptyListOf(first(), drop(1))

private data class PawNonEmptyListImpl<A>(
    override val first: A,
    override val tail: List<A>
) : PawNonEmptyList<A> {
    private val list = listOf(first) + tail

    override infix operator fun plus(other: List<A>): PawNonEmptyList<A> = PawNonEmptyListImpl(first, tail + other)

    override infix operator fun plus(other: A): PawNonEmptyList<A> = PawNonEmptyListImpl(first, tail + other)

    override fun toList(): List<A> = list
    override fun toSet(): Set<A> = setOf(first) + tail

    override fun <R> map(f: (A) -> R): PawNonEmptyList<R> = PawNonEmptyListImpl(f(first), tail.map(f))

    override fun <R> flatMap(f: (A) -> PawNonEmptyList<R>): PawNonEmptyList<R> {
        val firstList = f(first)
        val restOfList = tail.flatMap { f(it).toList() }
        return PawNonEmptyListImpl(firstList.first, firstList.tail + restOfList)
    }
}