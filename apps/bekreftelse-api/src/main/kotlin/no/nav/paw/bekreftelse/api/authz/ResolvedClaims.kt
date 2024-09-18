package no.nav.paw.bekreftelse.api.authz

import no.nav.security.token.support.core.context.TokenValidationContext

class ResolvedClaims private constructor(
    private val map: Map<Claim<*>, Any>
) {
    constructor() : this(emptyMap())

    override fun toString(): String {
        return map.map { (key, value) -> "${key}=$value" }
            .let { "ResolvedClaims($it)" }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(claim: Claim<T>): T? = map[claim] as T?

    fun <T : Any> add(claim: Claim<T>, rawValue: String): ResolvedClaims {
        val parsed = claim.fromString(rawValue)
        val pair: Pair<Claim<T>, Any> = claim to parsed
        return ResolvedClaims(map + pair)
    }

    fun isTokenX() = map.isNotEmpty() && map.keys.filter { it.issuer == TokenX }.size == map.size

    fun isAzure() = map.isNotEmpty() && map.keys.filter { it.issuer == Azure }.size == map.size

    fun isResolved(claim: Claim<*>): Boolean = map.containsKey(claim)

    fun isEmpty() = map.isEmpty()
}

fun TokenValidationContext?.resolveClaims(vararg claims: Claim<*>): ResolvedClaims =
    claims
        .mapNotNull { claim -> resolve(claim)?.let { claim to it } }
        .fold(ResolvedClaims()) { resolvedClaims, (claim, value) ->
            resolvedClaims.add(claim, value)
        }

fun TokenValidationContext?.resolve(claim: Claim<*>): String? =
    this?.getClaimOrNull(claim.issuer.name)
        ?.getStringClaim(claim.claimName)

fun TokenValidationContext.getClaimOrNull(issuer: String) =
    issuers
        .firstOrNull { it.equals(issuer, ignoreCase = true) }
        ?.let { presentIssuer -> getClaims(presentIssuer) }