package no.nav.paw.arbeidssoekerregisteret.testdata.internehendelser

import no.nav.paw.arbeidssoekerregisteret.testdata.KafkaKeyContext
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import java.util.*

fun KafkaKeyContext.startet(
    hendelseId: UUID = UUID.randomUUID(),
    identitetsnummer: String,
    arbeidssoekerId: Long = getKafkaKey(identitetsnummer).id,
    metadata: Metadata,
    opplysninger: Set<Opplysning>
): Startet =
    Startet(
        hendelseId = hendelseId,
        id = arbeidssoekerId,
        identitetsnummer = identitetsnummer,
        metadata = metadata,
        opplysninger = opplysninger
    )


fun test() {
    listOf(1,2,3).single()
}