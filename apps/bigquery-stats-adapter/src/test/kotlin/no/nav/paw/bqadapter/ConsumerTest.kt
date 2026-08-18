package no.nav.paw.bqadapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import java.time.Instant
import java.util.UUID

class ConsumerTest : FreeSpec({
    "RecordsByType" - {
        "should group opplysninger records" {
            val opplysninger = OpplysningerOmArbeidssoeker(
                UUID.fromString("c52ce702-c12f-49ab-a064-bb504613d680"),
                UUID.fromString("39542bbb-d6d1-472d-9776-78f0ebdf64d1"),
                Metadata(
                    Instant.parse("2026-08-18T10:00:00Z"),
                    Bruker(BrukerType.SYSTEM, "test", null),
                    "test",
                    "test",
                    null
                ),
                null,
                null,
                Jobbsituasjon(emptyList()),
                null
            )
            val record: Record<Any> = Record(
                id = "record-id",
                recordTimestamp = Instant.parse("2026-08-18T10:00:00Z"),
                value = opplysninger
            )

            RecordsByType(listOf(record))
                .get<OpplysningerOmArbeidssoeker>()
                .map { it.value } shouldContainExactly listOf(opplysninger)
        }
    }
})
