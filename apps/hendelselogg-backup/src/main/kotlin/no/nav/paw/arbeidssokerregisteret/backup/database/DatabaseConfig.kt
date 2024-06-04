package no.nav.paw.arbeidssokerregisteret.backup.database

data class DatabaseConfig(
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String
) {
    val url get() = "jdbc:postgresql://$host:$port/$name?user=$username&password=$password"
}