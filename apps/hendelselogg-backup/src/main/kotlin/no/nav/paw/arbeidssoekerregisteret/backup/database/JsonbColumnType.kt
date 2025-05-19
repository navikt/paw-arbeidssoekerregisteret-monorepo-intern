package no.nav.paw.arbeidssoekerregisteret.backup.database

import no.nav.paw.arbeidssoekerregisteret.backup.database.hendelse.HendelseTable.registerColumn
import org.jetbrains.exposed.sql.IColumnType
import org.postgresql.util.PGobject

fun jsonb(name: String) = registerColumn(name, JsonbColumnType(false))

class JsonbColumnType(override var nullable: Boolean): IColumnType<String> {

    override fun sqlType(): String = "JSONB"

    override fun valueFromDB(value: Any): String? {
        if (value is PGobject) {
            if (value.type.equals(sqlType(), true)) {
                return value.value
            } else {
                throw IllegalArgumentException("Value is not a JSONB object: ${value.type}")
            }
        } else {
            throw IllegalArgumentException("Value is not a PGobject: ${value.javaClass}")
        }
    }

    override fun valueToDB(value: String?): Any {
        return PGobject().apply {
            type = sqlType()
            this.value = value
        }
    }
}