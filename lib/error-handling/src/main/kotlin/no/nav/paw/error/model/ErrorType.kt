package no.nav.paw.error.model

import java.net.URI

object ErrorTypeDefaults {
    const val TEAM = "paw"
    const val DOMAIN = "default"
    const val ERROR = "ukjent-feil"
}

class ErrorType(
    var team: String = ErrorTypeDefaults.TEAM,
    var domain: String = ErrorTypeDefaults.DOMAIN,
    var error: String = ErrorTypeDefaults.ERROR,
) {
    fun team(team: String) = apply { this.team = team }
    fun domain(domain: String) = apply { this.domain = domain }
    fun error(error: String) = apply { this.error = error }
    fun build(): URI = URI.create("urn:${team.lowercase()}:${domain.lowercase()}:${error.lowercase()}")

    companion object {
        fun team(team: String): ErrorType = ErrorType(team = team)
        fun domain(domain: String): ErrorType = ErrorType(domain = domain)
        fun error(error: String): ErrorType = ErrorType(error = error)
        fun default(): ErrorType = ErrorType()
    }
}

fun String.asHttpErrorType(): URI = ErrorType.domain("http").error(this).build()
fun String.asSecurityErrorType(): URI = ErrorType.domain("security").error(this).build()
