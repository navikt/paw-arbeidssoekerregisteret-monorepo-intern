package no.nav.paw.arbeidssokerregisteret.utils

import no.nav.security.token.support.core.context.TokenValidationContext

class ResolvedClaims private constructor(
    private val map: Map<Claim<*>, Any>
) {
    constructor() : this(emptyMap())

    val issuer: String = map.keys.firstOrNull()?.issuer ?: "undefined"

    override fun toString(): String {
        return map.map { (key, value) -> "${key}=$value" }
            .let { "ResolvedClaims($it)" }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(claim: Claim<T>): T? = map[claim] as T?
    fun isResolved(claim: Claim<*>): Boolean = map.containsKey(claim)

    fun <T : Any> add(claim: Claim<T>, value: Any): ResolvedClaims {
        val parsed = claim.resolve(value)
        val pair: Pair<Claim<T>, Any> = claim to parsed
        return ResolvedClaims(map + pair)
    }

}

fun TokenValidationContext?.resolveClaims(vararg claims: Claim<*>): ResolvedClaims =
    claims
        .mapNotNull { claim -> resolve(claim)?.let { claim to it } }
        .fold(ResolvedClaims()) { resolvedClaims, (claim, value) ->
            resolvedClaims.add(claim, value)
        }

fun TokenValidationContext?.resolve(claim: Claim<*>): Any? {
    return when (claim) {
        is ListClaim<*> -> {
            this?.getClaimOrNull(claim.issuer)
                ?.getAsList(claim.claimName)
        }

        else -> {
            this?.getClaimOrNull(claim.issuer)
                ?.getStringClaim(claim.claimName)
        }
    }
}

fun TokenValidationContext.getClaimOrNull(issuer: String) =
    issuers
        .firstOrNull { it.equals(issuer, ignoreCase = true) }
        ?.let { presentIssuer -> getClaims(presentIssuer) }