package no.nav.paw.kafkakeygenerator.vo

sealed interface Either<out L, out R> {
    fun <R2> map(f: (R) -> R2): Either<L, R2>
    fun <L2> mapLeft(f: (L) -> L2): Either<L2, R>
    fun onLeft(f: (L) -> Unit): Either<L, R>
    fun onRight(f: (R) -> Unit): Either<L, R>
    fun <V> fold(onLeft: (L) -> V, onRight: (R) -> V): V
}

fun <L> left(value: L): Left<L> = Left(value)
fun <R> right(value: R): Right<R> = Right(value)

fun <L1, R1, R2> Either<L1, R1>.flatMap(f: (R1) -> Either<L1, R2>): Either<L1, R2> = when (this) {
    is Right -> f(right)
    is Left -> this
}

data class Right<R>(val right: R) : Either<Nothing, R> {
    override fun <R2> map(f: (R) -> R2): Right<R2> = Right(f(right))
    override fun <L2> mapLeft(f: (Nothing) -> L2): Right<R> = this
    override fun onLeft(f: (Nothing) -> Unit) = this
    override fun onRight(f: (R) -> Unit) = this.also { f(right) }

    override fun <V> fold(onLeft: (Nothing) -> V, onRight: (R) -> V): V = onRight(right)
}

data class Left<L>(val left: L) : Either<L, Nothing> {
    override fun <R2> map(f: (Nothing) -> R2): Left<L> = this
    override fun <L2> mapLeft(f: (L) -> L2): Left<L2> = Left(f(left))
    override fun onLeft(f: (L) -> Unit) = this.also { f(left) }
    override fun onRight(f: (Nothing) -> Unit) = this
    override fun <V> fold(onLeft: (L) -> V, onRight: (Nothing) -> V): V = onLeft(left)
}

fun <L, R, C : Collection<Either<L, R>>> Either<L, C>.flatten(): Either<L, List<R>> {
    return flatMap { list ->
        val lefts = list.filterIsInstance<Left<L>>()
        if (lefts.isNotEmpty()) {
            lefts.first()
        } else {
            list.filterIsInstance<Right<R>>()
                .map { it.right }
                .let(::right)
        }
    }
}

fun <L, R, C : Collection<Either<L, R>>> C.flatten(): Either<L, List<R>> {
    val lefts = filterIsInstance<Left<L>>()
    return if (lefts.isNotEmpty()) {
        lefts.first()
    } else {
        filterIsInstance<Right<R>>()
            .map { it.right }
            .let(::right)
    }
}
