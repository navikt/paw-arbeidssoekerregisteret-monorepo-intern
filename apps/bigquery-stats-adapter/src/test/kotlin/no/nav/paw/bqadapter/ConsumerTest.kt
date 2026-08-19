package no.nav.paw.bqadapter

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
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

        "should group profiling records" {
            val profilering = Profilering(
                UUID.fromString("9d3fa766-cca6-4825-9841-bfb63d0ccac2"),
                UUID.fromString("39542bbb-d6d1-472d-9776-78f0ebdf64d1"),
                UUID.fromString("c52ce702-c12f-49ab-a064-bb504613d680"),
                Metadata(
                    Instant.parse("2026-08-19T06:00:00Z"),
                    Bruker(BrukerType.SYSTEM, "profilering:1", null),
                    "profilering",
                    "opplysninger_mottatt",
                    null
                ),
                ProfilertTil.ANTATT_GODE_MULIGHETER,
                true,
                42
            )
            val record: Record<Any> = Record(
                id = "record-id",
                recordTimestamp = Instant.parse("2026-08-19T06:00:00Z"),
                value = profilering
            )

            RecordsByType(listOf(record))
                .get<Profilering>()
                .map { it.value } shouldContainExactly listOf(profilering)
        }
    }
})
