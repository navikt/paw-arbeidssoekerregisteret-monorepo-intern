package no.nav.paw.kafkakeygenerator.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addResourceSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.lang.System.getenv

@OptIn(ExperimentalHoplite::class)
inline fun <reified A> lastKonfigurasjon(navn: String): A {
    val fulltNavn = when (getenv("NAIS_CLUSTER_NAME")) {
        "prod-gcp" -> "/prod/$navn"
        "dev-gcp" -> "/dev/$navn"
        else -> "/local/$navn"
    }
    return ConfigLoaderBuilder
        .default()
        .withExplicitSealedTypes()
        .addResourceSource(fulltNavn)
        .build()
        .loadConfigOrThrow()
}

fun DatabaseKonfigurasjon.dataSource() =
    HikariDataSource(HikariConfig().apply {
        jdbcUrl = url
        driverClassName = "org.postgresql.Driver"
        password = passord
        username = brukernavn
        isAutoCommit = false
    })