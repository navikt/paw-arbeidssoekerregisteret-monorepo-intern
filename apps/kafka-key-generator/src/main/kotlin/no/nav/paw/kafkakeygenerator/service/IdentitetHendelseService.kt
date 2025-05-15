package no.nav.paw.kafkakeygenerator.service

import no.nav.paw.identitet.internehendelser.IdentitetHendelseDeserializer
import no.nav.paw.identitet.internehendelser.IdentitetHendelseSerializer
import no.nav.paw.identitet.internehendelser.IdentiteterEndretHendelse
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.model.IdentitetHendelseStatus
import no.nav.paw.kafkakeygenerator.repository.IdentitetHendelseRepository
import no.nav.paw.logging.logger.buildLogger

class IdentitetHendelseService(
    private val identitetHendelseRepository: IdentitetHendelseRepository
) {
    private val logger = buildLogger
    private val serializer = IdentitetHendelseSerializer()
    private val deserializer = IdentitetHendelseDeserializer()

    fun lagreVentendeIdentitetHendelse(
        aktorId: String,
        arbeidssoekerId: Long,
        identiteter: MutableList<Identitet>,
        tidligereIdentiteter: MutableList<Identitet>
    ) {
        val arbeidssoekerIdIdentitet = Identitet(
            identitet = arbeidssoekerId.toString(),
            type = IdentitetType.ARBEIDSSOEKERID,
            gjeldende = true
        )
        if (identiteter.isNotEmpty()) {
            identiteter.add(arbeidssoekerIdIdentitet)
        }
        if (tidligereIdentiteter.isNotEmpty()) {
            tidligereIdentiteter.add(arbeidssoekerIdIdentitet)
        }
        val hendelse = IdentiteterEndretHendelse(
            identiteter = identiteter,
            tidligereIdentiteter = tidligereIdentiteter
        )
        val rowsAffected = identitetHendelseRepository.insert(
            arbeidssoekerId = arbeidssoekerId,
            aktorId = aktorId,
            data = serializer.serializeToString(hendelse),
            status = IdentitetHendelseStatus.VENTER
        )
        logger.info("Lagret utgående identitet-hendelse (rows affected {})", rowsAffected)
    }

    fun sendVentendeIdentitetHendelser() {
        val hendelser = identitetHendelseRepository.findByStatusForUpdate(
            status = IdentitetHendelseStatus.VENTER,
            limit = 100
        )
        identitetHendelseRepository.updateStatusByIdList(
            idList = hendelser.map { it.id },
            status = IdentitetHendelseStatus.PROSESSERER
        )
    }
}