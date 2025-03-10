package no.nav.paw.arbeidssoekerregisteret.app.vo

import no.nav.paw.arbeidssoekerregisteret.app.ApplicationInfo
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import java.time.Instant
import java.util.*

data class GyldigHendelse(
    val id: Long,
    val foedselsnummer: Foedselsnummer,
    val formidlingsgruppe: Formidlingsgruppe,
    val formidlingsgruppeEndret: Instant,
)

data class Formidlingsgruppe(val kode: String)

data class Foedselsnummer(val foedselsnummer: String)

fun avsluttet(topic: String, periodeId: UUID, hendelse: GyldigHendelse): Avsluttet =
    Avsluttet(
        hendelseId = UUID.randomUUID(),
        id = hendelse.id,
        identitetsnummer = hendelse.foedselsnummer.foedselsnummer,
        metadata = Metadata(
            tidspunkt = Instant.now(),
            aarsak = hendelse.formidlingsgruppe.kode,
            kilde = "topic:$topic",
            utfoertAv = Bruker(
                type = BrukerType.SYSTEM,
                id = ApplicationInfo.id,
                sikkerhetsnivaa = null
            ),
            tidspunktFraKilde = TidspunktFraKilde(
                tidspunkt = hendelse.formidlingsgruppeEndret,
                avviksType = AvviksType.FORSINKELSE
            )
        ),
        periodeId = periodeId
    )

