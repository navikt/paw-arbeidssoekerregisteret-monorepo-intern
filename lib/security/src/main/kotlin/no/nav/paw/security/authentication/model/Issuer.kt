package no.nav.paw.security.authentication.model

sealed class Issuer(val name: String)

data object TokenX : Issuer("tokenx")
data object AzureAd : Issuer("azure")
data object IdPorten : Issuer("idporten")
data object MaskinPorten : Issuer("maskinporten")
