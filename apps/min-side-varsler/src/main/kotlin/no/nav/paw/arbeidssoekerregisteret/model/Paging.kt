package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.sql.SortOrder

enum class Order {
    ASC, DESC
}

class Paging private constructor(
    val offset: Long,
    val size: Int,
    val order: Order
) {
    fun stepBySize(): Paging = Paging(
        offset = offset + size,
        size = size,
        order = order
    )

    fun stepByPage(page: Int): Paging = Paging(
        offset = ((page - 1) * size).toLong(),
        size = size,
        order = order
    )

    override fun toString(): String {
        return "Paging(offset=$offset, size=$size, order=$order)"
    }

    companion object {
        fun of(offset: Long, size: Int, order: Order): Paging = Paging(
            offset = offset,
            size = size,
            order = order
        )

        fun none(): Paging = Paging(
            offset = 0,
            size = Int.MAX_VALUE,
            order = Order.DESC
        )
    }
}

fun Order.asSortOrder() = when (this) {
    Order.ASC -> SortOrder.ASC
    Order.DESC -> SortOrder.DESC
}
