package no.nav.paw.arbeidssokerregisteret.utils

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
    operator fun <T> get(claim: Claim<T>): T? = map[claim] as T?
    fun isResolved(claim: Claim<*>): Boolean = map.containsKey(claim)

    fun <T> add(claim: Claim<T>, rawValue: String): ResolvedClaims =
        ResolvedClaims(map + (claim to claim.fromString))
}

fun TokenValidationContext?.resolveClaims(vararg claims: Claim<*>): ResolvedClaims =
    claims
        .mapNotNull { claim -> resolve(claim)?.let { claim to it } }
        .fold(ResolvedClaims()) { resolvedClaims, (claim, value) ->
            resolvedClaims.add(claim, value)
        }

fun <T> format(claim: Claim<T>, value: String): T = claim.fromString(value)

fun TokenValidationContext?.resolve(claim: Claim<*>): String? =
    this?.getClaims(claim.issuer)
        ?.getStringClaim(claim.claimName)
