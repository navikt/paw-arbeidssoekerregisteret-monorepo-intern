package no.nav.paw.arbeidssoekerregisteret.model

import java.util.*

data class AvsluttVarsel(
    override val varselId: UUID,
    override val value: String
) : VarselMelding