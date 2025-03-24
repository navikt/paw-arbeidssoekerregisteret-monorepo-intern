package no.nav.paw.arbeidssoekerregisteret.bekreftelse.backup.database

import javax.sql.DataSource

data class DatabaseConfig(
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val jdbc: String?
) {
    val url get() = jdbc ?: "jdbc:postgresql://$host:$port/$name?user=$username&password=$password"

    override fun toString(): String {
        return if (jdbc != null) {
            "DatabaseConfig(name='$name', jdbcUrl='***')"
        } else {
            "DatabaseConfig(name='$name', host='$host', port=$port, username='$username', password='${if (password.isBlank()) "null" else "***"}')"
        }
    }
}

fun DatabaseConfig.dataSource(): DataSource {
    val dataSource = org.postgresql.ds.PGSimpleDataSource()
    dataSource.setURL(url)
    return dataSource
}