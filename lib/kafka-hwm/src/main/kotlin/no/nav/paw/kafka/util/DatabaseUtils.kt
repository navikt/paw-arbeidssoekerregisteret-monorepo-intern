package no.nav.paw.kafka.util

import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.wrap

infix fun <T> ExpressionWithColumnType<T>.max(t: T): Greatest<T> = Greatest(
    expr1 = this,
    expr2 = wrap(t),
    columnType = this.columnType
)

class Greatest<T>(
    expr1: Expression<T>,
    expr2: Expression<T>,
    columnType: IColumnType<T & Any>
) : CustomFunction<T>(
    functionName = "greatest",
    columnType = columnType,
    expr1,
    expr2
)