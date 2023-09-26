package no.nav.paw.arbeidssokerregisteret.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.paw.arbeidssokerregisteret.utils.konfigVerdi
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

class DatabaseKonfigurasjon(env: Map<String, String>) {
    private val prefix = "NAIS_DATABASE_PAW_ARBEIDSSOKER_REGISTERET_ARBEIDSSOKERREGISTERET"
    private val host: String = env.konfigVerdi("${prefix}_HOST")
    private val port: Int = env.konfigVerdi("${prefix}_PORT", String::toInt)
    private val brukernavn: String = env.konfigVerdi("${prefix}_USERNAME")
    private val passord: String = env.konfigVerdi("${prefix}_PASSWORD")
    private val databaseNavn: String = env.konfigVerdi("${prefix}_DATABASE")

    private val url get() = "jdbc:postgresql://$host:$port/$databaseNavn?user=$brukernavn&password=$passord"
    val dataSource
        get() = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = url
                driverClassName = "org.postgresql.Driver"
                password = passord
                username = brukernavn
            },
        )
}

fun DatabaseKonfigurasjon.migrateDatabase() = Flyway.configure()
    .dataSource(dataSource)
    .baselineOnMigrate(true)
    .load()
    .migrate()

fun DatabaseKonfigurasjon.connect() = Database.connect(dataSource)
