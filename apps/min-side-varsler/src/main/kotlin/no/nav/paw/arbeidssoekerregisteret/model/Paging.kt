package no.nav.paw.arbeidssoekerregisteret.model

import org.jetbrains.exposed.sql.SortOrder

data class Paging(
    val size: Int = Int.MAX_VALUE,
    val offset: Long = 0,
    val ordering: SortOrder = SortOrder.DESC
)
