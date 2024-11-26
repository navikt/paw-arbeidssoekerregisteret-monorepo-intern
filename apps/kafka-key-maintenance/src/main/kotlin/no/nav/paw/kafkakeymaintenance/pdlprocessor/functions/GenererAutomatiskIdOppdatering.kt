package no.nav.paw.kafkakeymaintenance.pdlprocessor.functions

import no.nav.paw.kafkakeymaintenance.vo.AutomatiskIdOppdatering
import no.nav.paw.kafkakeymaintenance.vo.IdOppdatering
import no.nav.paw.kafkakeymaintenance.perioder.PeriodeRad
import no.nav.paw.kafkakeymaintenance.vo.AvviksMelding
import no.nav.paw.kafkakeymaintenance.vo.IdMap
import org.slf4j.LoggerFactory

private val avviksMeldingerLogger = LoggerFactory.getLogger("avvik.generer_automatisk_id_oppdatering")
fun genererAutomatiskIdOppdatering(avvik: AvviksMelding, periodeRad: PeriodeRad): IdOppdatering {
    requireNotNull(avvik.lokaleAlias.find { it.identitetsnummer == periodeRad.identitetsnummer }) {
        "Intern logiskfeil, lokal data for identietetsnummer mangler"
    }.let { alias ->
        return AutomatiskIdOppdatering(
            oppdatertData = IdMap(
                gjeldeneIdentitetsnummer = avvik.gjeldeneIdentitetsnummer,
                arbeidsoekerId = alias.arbeidsoekerId,
                identitetsnummer = alias.identitetsnummer,
                recordKey = alias.recordKey,
                partisjon = alias.partition,
                identiteter = avvik.lokaleAliasSomSkalPekePaaPdlPerson()
            ),
            frieIdentiteter = avvik.lokaleAliasSomIkkeSkalPekePaaPdlPerson()
        ).also {
            avviksMeldingerLogger.debug(
                "Generert automatisk id oppdatering: alias som skal peke på pdl person: {}, frie identiteter: {}",
                it.oppdatertData?.identiteter?.size,
                it.frieIdentiteter.size
            )
        }
    }

}

fun genererAutomatiskIdOppdatering(avvik: AvviksMelding): IdOppdatering {
    val frieIdentiteter = avvik.lokaleAliasSomIkkeSkalPekePaaPdlPerson()
    val identerSomSkalPekePaaPdlPerson = avvik.lokaleAliasSomSkalPekePaaPdlPerson()
    return if (identerSomSkalPekePaaPdlPerson.isEmpty()) {
        AutomatiskIdOppdatering(
            oppdatertData = null,
            frieIdentiteter = frieIdentiteter
        )
    } else {
        val alias = identerSomSkalPekePaaPdlPerson.maxBy { it.arbeidsoekerId }
        AutomatiskIdOppdatering(
            oppdatertData = IdMap(
                gjeldeneIdentitetsnummer = avvik.gjeldeneIdentitetsnummer,
                arbeidsoekerId = alias.arbeidsoekerId,
                identitetsnummer = alias.identitetsnummer,
                recordKey = alias.recordKey,
                partisjon = alias.partition,
                identiteter = identerSomSkalPekePaaPdlPerson
            ),
            frieIdentiteter = frieIdentiteter
        )
    }
}