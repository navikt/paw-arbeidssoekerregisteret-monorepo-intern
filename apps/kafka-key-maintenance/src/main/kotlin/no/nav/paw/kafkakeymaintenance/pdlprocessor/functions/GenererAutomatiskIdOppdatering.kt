package no.nav.paw.kafkakeymaintenance.pdlprocessor.functions

import no.nav.paw.kafkakeymaintenance.vo.AutomatiskIdOppdatering
import no.nav.paw.kafkakeymaintenance.vo.IdOppdatering
import no.nav.paw.kafkakeymaintenance.perioder.PeriodeRad
import no.nav.paw.kafkakeymaintenance.vo.AvviksMelding
import no.nav.paw.kafkakeymaintenance.vo.IdMap

fun genererAutomatiskIdOppdatering(avvik: AvviksMelding, periodeRad: PeriodeRad): IdOppdatering {
    requireNotNull(avvik.lokaleAlias.find { it.identitetsnummer == periodeRad.identitetsnummer }) {
        "Intern logiskfeil, lokal data for identietetsnummer mangler"
    }.let { alias ->
        return AutomatiskIdOppdatering(
            oppdatertData = IdMap(
                gjeldeneIdentitetsnummer = avvik.gjeldeneIdentitetsnummer,
                arbeidsoekerId = alias.arbeidsoekerId,
                recordKey = alias.recordKey,
                partisjon = alias.partition,
                identiteter = avvik.lokaleAliasSomSkalPekePaaPdlPerson()
            ),
            frieIdentiteter = avvik.lokaleAliasSomIkkeSkalPekePaaPdlPerson()
        )
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
        val data = identerSomSkalPekePaaPdlPerson.maxBy { it.arbeidsoekerId }
        AutomatiskIdOppdatering(
            oppdatertData = IdMap(
                gjeldeneIdentitetsnummer = avvik.gjeldeneIdentitetsnummer,
                arbeidsoekerId = data.arbeidsoekerId,
                recordKey = data.recordKey,
                partisjon = data.partition,
                identiteter = identerSomSkalPekePaaPdlPerson
            ),
            frieIdentiteter = frieIdentiteter
        )
    }
}