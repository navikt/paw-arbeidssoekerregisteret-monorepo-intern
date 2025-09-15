package no.nav.paw.kafkakeygenerator.api.v2

import no.nav.paw.kafkakeygenerator.mergedetector.vo.MergeDetected
import no.nav.paw.kafkakeygenerator.model.Info

data class InfoResponse(
    val info: Info,
    val mergeDetected: MergeDetected?
)

data class AliasResponse(
    val alias: List<LokaleAlias>
)

data class LokaleAlias(
    val identitetsnummer: String,
    val koblinger: List<Alias>
)

data class Alias(
    val identitetsnummer: String,
    val arbeidsoekerId: Long,
    val recordKey: Long,
    val partition: Int,
)