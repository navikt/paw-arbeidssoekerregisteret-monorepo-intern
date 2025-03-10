package no.nav.paw.security.authentication.model

sealed class Token(val issuer: Issuer, val claims: List<Claim<*>>)

data object TokenXToken : Token(TokenX, listOf(PID, ACR))
data object AzureAdToken : Token(AzureAd, listOf(OID, Name, NavIdent, Roles, ACR))
data object IdPortenToken : Token(IdPorten, listOf(PID, ACR))
data object MaskinPortenToken : Token(MaskinPorten, listOf(ACR))

val validTokens: List<Token> = listOf(TokenXToken, AzureAdToken, IdPortenToken, MaskinPortenToken)
