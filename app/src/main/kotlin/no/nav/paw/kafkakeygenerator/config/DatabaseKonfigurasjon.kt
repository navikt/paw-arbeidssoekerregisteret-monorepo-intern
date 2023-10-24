package no.nav.paw.kafkakeygenerator.config

data class DatabaseKonfigurasjon(
    val host: String,
    val port: Int,
    val brukernavn: String,
    val passord: String,
    val databasenavn: String,
) {
    val url get() = "jdbc:postgresql://$host:$port/$databasenavn?user=$brukernavn&password=$passord"
}