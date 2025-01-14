package no.nav.paw.security.authentication.model

sealed class Token(val issuer: Issuer, val claims: List<Claim<*>>)

data object TokenXToken : Token(TokenX, listOf(PID))
data object AzureAdToken : Token(AzureAd, listOf(OID, Name, NavIdent, Roles))
data object IdPortenToken : Token(IdPorten, listOf(PID))
data object MaskinPortenToken : Token(MaskinPorten, emptyList())

val validTokens: List<Token> = listOf(TokenXToken, AzureAdToken, IdPortenToken, MaskinPortenToken)
