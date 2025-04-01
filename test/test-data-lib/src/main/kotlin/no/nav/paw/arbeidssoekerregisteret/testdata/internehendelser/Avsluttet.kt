package no.nav.paw.arbeidssoekerregisteret.testdata.internehendelser

import no.nav.paw.arbeidssoekerregisteret.testdata.KafkaKeyContext
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Aarsak
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import java.util.*

fun KafkaKeyContext.avsluttet(
    hendelseId: UUID = UUID.randomUUID(),
    identitetsnummer: String = "12345678901",
    arbeidssoekerId: Long = getKafkaKey(identitetsnummer).id,
    opplysninger: Set<Opplysning> = emptySet()
): Avsluttet = Avsluttet(
    hendelseId = hendelseId,
    id = arbeidssoekerId,
    identitetsnummer = identitetsnummer,
    metadata = metadata(),
    opplysninger = opplysninger,
    kalkulertAarsak = Aarsak.Udefinert,
    oppgittAarsak = Aarsak.Udefinert
)