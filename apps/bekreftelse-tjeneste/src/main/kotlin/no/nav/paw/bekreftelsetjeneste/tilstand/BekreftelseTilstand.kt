package no.nav.paw.bekreftelsetjeneste.tilstand

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = IkkeKlarForUtfylling::class, name = "IkkeKlarForUtfylling"),
    JsonSubTypes.Type(value = KlarForUtfylling::class, name = "KlarForUtfylling"),
    JsonSubTypes.Type(value = VenterSvar::class, name = "VenterSvar"),
    JsonSubTypes.Type(value = GracePeriodeUtloept::class, name = "GracePeriodeUtloept"),
    JsonSubTypes.Type(value = Levert::class, name = "Levert")
)
sealed interface BekreftelseTilstand {
    val timestamp: Instant
}

data class GracePeriodeUtloept(override val timestamp: Instant) : BekreftelseTilstand
data class GracePeriodeVarselet(override val timestamp: Instant) : BekreftelseTilstand
data class IkkeKlarForUtfylling(override val timestamp: Instant) : BekreftelseTilstand
data class KlarForUtfylling(override val timestamp: Instant) : BekreftelseTilstand
data class Levert(override val timestamp: Instant) : BekreftelseTilstand
data class VenterSvar(override val timestamp: Instant) : BekreftelseTilstand
data class InternBekreftelsePaaVegneAvStartet(override val timestamp: Instant) : BekreftelseTilstand