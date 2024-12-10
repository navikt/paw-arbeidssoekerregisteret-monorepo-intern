package no.nav.paw.tilgangskontroll.vo

import no.nav.paw.tilgangskontroll.api.models.TilgangskontrollRequestV1

enum class Tilgang {
    LESE, SKRIVE, LESE_SKRIVE;

    companion object {
        fun valueOf(value: TilgangskontrollRequestV1.Tilgang): Tilgang =
            when (value) {
                TilgangskontrollRequestV1.Tilgang.LESE -> LESE
                TilgangskontrollRequestV1.Tilgang.SKRIVE -> SKRIVE
                TilgangskontrollRequestV1.Tilgang.LESE_SKRIVE -> LESE_SKRIVE
            }
    }
}