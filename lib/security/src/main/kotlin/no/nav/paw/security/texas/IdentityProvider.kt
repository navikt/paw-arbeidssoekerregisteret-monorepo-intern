package no.nav.paw.security.texas

enum class IdentityProvider(val value: String) {
    //"TokenX brukes ved utveksling av token for brukere/borgere"
    TOKEN_X("tokenx"),
    //"Azure AD brukes ved utveksling av token for internt ansatte"
    AZURE_AD("azuread"),
}