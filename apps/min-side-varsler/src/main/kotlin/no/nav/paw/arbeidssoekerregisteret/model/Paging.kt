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
    fun advance(): Paging = Paging(
        offset = offset + size,
        size = size,
        order = order
    )

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
