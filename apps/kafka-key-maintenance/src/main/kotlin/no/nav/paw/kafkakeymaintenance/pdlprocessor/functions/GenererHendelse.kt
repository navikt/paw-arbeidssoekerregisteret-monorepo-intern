package no.nav.paw.kafkakeymaintenance.pdlprocessor.functions

import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerOpphoert
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.kafkakeymaintenance.vo.AutomatiskIdOppdatering
import no.nav.paw.kafkakeymaintenance.vo.IdMap
import no.nav.paw.kafkakeymaintenance.vo.IdOppdatering
import no.nav.paw.kafkakeymaintenance.vo.ManuellIdOppdatering
import java.util.*

data class HendelseRecord<V: Hendelse>(
    val key: Long,
    val hendelse: V
)

fun genererHendelser(metadata: Metadata, idOppdatering: IdOppdatering): List<HendelseRecord<Hendelse>> {
    return when (idOppdatering) {
        is AutomatiskIdOppdatering -> genererHendelse(metadata, idOppdatering)
        is ManuellIdOppdatering -> emptyList()
    }
}

fun genererHendelse(metadata: Metadata, idOppdatering: AutomatiskIdOppdatering): List<HendelseRecord<Hendelse>> {
    val identitetsnummerOpphoert = idOppdatering
        .frieIdentiteter
        .groupBy { it.arbeidsoekerId }
        .map { (arbeidsoekerId, alias) ->
            val identiteter = alias.map { it.identitetsnummer }
            val hendelse: Hendelse = IdentitetsnummerOpphoert(
                id = arbeidsoekerId,
                hendelseId = UUID.randomUUID(),
                identitetsnummer = identiteter.first(),
                metadata = metadata,
                alleIdentitetsnummer = identiteter
            )
            HendelseRecord(alias.first().recordKey, hendelse)
        }
    val identitetsnummerSammenslaatt = idOppdatering.oppdatertData?.let { genererHendelse(metadata, it) } ?: emptyList()
    return identitetsnummerSammenslaatt + identitetsnummerOpphoert
}

fun genererHendelse(metadata: Metadata, idMap: IdMap): List<HendelseRecord<Hendelse>> =
    idMap.identiteter
        .filter { it.arbeidsoekerId != idMap.arbeidsoekerId }
        .groupBy { it.arbeidsoekerId }
        .map { (arbeidsoekerId, alias) ->
            val identiteter = alias.map { it.identitetsnummer }
            val hendelse = IdentitetsnummerSammenslaatt(
                id = arbeidsoekerId,
                hendelseId = UUID.randomUUID(),
                identitetsnummer = identiteter.first(),
                metadata = metadata,
                alleIdentitetsnummer = identiteter,
                flyttetTilArbeidssoekerId = idMap.arbeidsoekerId
            )
            HendelseRecord(alias.first().recordKey, hendelse)
        }