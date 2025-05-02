package no.nav.paw.kafkakeygenerator.utils

import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.wrap

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