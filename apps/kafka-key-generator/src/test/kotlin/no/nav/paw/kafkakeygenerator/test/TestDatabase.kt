package no.nav.paw.kafkakeygenerator.test

import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.dao.KonfliktIdentiteterTable
import no.nav.paw.kafkakeygenerator.model.dao.KonflikterTable
import no.nav.paw.logging.logger.buildNamedLogger
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.sql.SQLException
import java.time.Instant
import javax.sql.DataSource

private val logger = buildNamedLogger("database.test")

object TestDatabase {
    fun insertKonflikt(
        aktorId: String,
        type: KonfliktType,
        status: KonfliktStatus,
        sourceTimestamp: Instant,
        insertedTimestamp: Instant = Instant.now(),
        updatedTimestamp: Instant? = null,
        identiteter: List<Identitet>
    ): Int = runCatching {
        transaction {
            val statement = KonflikterTable.insert {
                it[KonflikterTable.aktorId] = aktorId
                it[KonflikterTable.type] = type
                it[KonflikterTable.status] = status
                it[KonflikterTable.sourceTimestamp] = sourceTimestamp
                it[KonflikterTable.insertedTimestamp] = insertedTimestamp
                it[KonflikterTable.updatedTimestamp] = updatedTimestamp
            }
            val id = statement[KonflikterTable.id]
            val identiteterInsertedCount = identiteter
                .sumOf { KonfliktIdentiteterTable.insert(id.value, it) }
            statement.insertedCount + identiteterInsertedCount
        }
    }.getOrElse { throwable ->
        logger.error("Feil ved insert av konflikt-identitet", throwable)
        throw SQLException("Feil ved insert av konflikt-identitet")
    }
}

fun String.runAsSql() {
    val sql = this
    transaction {
        exec(sql)
    }
}

fun buildPostgresDataSource(): DataSource {
    val config = postgreSQLContainer().let {
        DatabaseConfig(
            host = it.host,
            port = it.firstMappedPort,
            database = it.databaseName,
            username = it.username,
            password = it.password,
            autoCommit = false
        )
    }
    return createHikariDataSource(config)
}

private fun postgreSQLContainer(): PostgreSQLContainer<out PostgreSQLContainer<*>> {
    val postgres = PostgreSQLContainer(
        "postgres:14"
    ).apply {
        addEnv("POSTGRES_PASSWORD", "admin")
        addEnv("POSTGRES_USER", "admin")
        addEnv("POSTGRES_DATABASE", "pawkafkakeys")
        addExposedPorts(5432)
    }
    postgres.start()
    postgres.waitingFor(Wait.forHealthcheck())
    return postgres
}
