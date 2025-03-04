package no.nav.paw.arbeidssoekerregisteret.model

import java.util.*

data class OpprettBeskjed(
    override val varselId: UUID,
    override val value: String
) : VarselMelding