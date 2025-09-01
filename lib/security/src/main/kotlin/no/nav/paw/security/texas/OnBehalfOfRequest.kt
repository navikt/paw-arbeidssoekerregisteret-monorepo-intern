package no.nav.paw.security.texas

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.paw.security.texas.IdentityProvider.AZURE_AD
import no.nav.paw.security.texas.IdentityProvider.TOKEN_X

sealed interface OnBehalfOfRequest {
    val userToken: String
    val target: String
    val identityProvider: String
}

data class OnBehalfOfBrukerRequest(
    @field:JsonProperty("user_token")
    override val userToken: String,
    override val target: String,
) : OnBehalfOfRequest {
    @field:JsonProperty("identity_provider")
    override val identityProvider: String = TOKEN_X.value
}

data class OnBehalfOfAnsattRequest(
    @field:JsonProperty("user_token")
    override val userToken: String,
    override val target: String,
) : OnBehalfOfRequest {
    @field:JsonProperty("identity_provider")
    override val identityProvider: String = AZURE_AD.value
}
