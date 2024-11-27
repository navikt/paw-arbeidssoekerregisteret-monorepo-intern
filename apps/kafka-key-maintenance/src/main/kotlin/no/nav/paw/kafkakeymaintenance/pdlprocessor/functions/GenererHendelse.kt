package no.nav.paw.kafkakeymaintenance.pdlprocessor.functions

import no.nav.paw.arbeidssokerregisteret.intern.v1.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.kafkakeymaintenance.perioder.PeriodeRad
import no.nav.paw.kafkakeymaintenance.vo.AutomatiskIdOppdatering
import no.nav.paw.kafkakeymaintenance.vo.IdMap
import no.nav.paw.kafkakeymaintenance.vo.IdOppdatering
import no.nav.paw.kafkakeymaintenance.vo.ManuellIdOppdatering
import org.slf4j.LoggerFactory
import java.util.*

data class HendelseRecord<V: Hendelse>(
    val key: Long,
    val hendelse: V
)
private val logger = LoggerFactory.getLogger("id.oppdatering.manuell")
fun genererHendelser(metadata: Metadata, idOppdatering: IdOppdatering): List<HendelseRecord<Hendelse>> {
    return when (idOppdatering) {
        is AutomatiskIdOppdatering -> {
            idOppdatering.oppdatertData?.let { genererHendelse(metadata, it) } ?: emptyList()
        }

        is ManuellIdOppdatering -> {
            logger.warn("Manuell id oppdatering oppdaget")
            genererHendelse(metadata, idOppdatering)
        }
    }
}

fun genererHendelse(metadata: Metadata, idMap: IdMap): List<HendelseRecord<Hendelse>> =
    idMap.identiteter
        .filter { it.arbeidsoekerId != idMap.arbeidsoekerId }
        .groupBy { it.arbeidsoekerId }
        .flatMap { (arbeidsoekerId, alias) ->
            val identiteter = alias.map { it.identitetsnummer }.toSet()
            val hendelse = IdentitetsnummerSammenslaatt(
                id = arbeidsoekerId,
                hendelseId = UUID.randomUUID(),
                identitetsnummer = identiteter.first(),
                metadata = metadata,
                flyttedeIdentitetsnumre = identiteter,
                flyttetTilArbeidssoekerId = idMap.arbeidsoekerId
            )
            val infoHendelse = ArbeidssoekerIdFlettetInn(
                identitetsnummer = idMap.identitetsnummer,
                id = idMap.arbeidsoekerId,
                hendelseId = UUID.randomUUID(),
                metadata = metadata,
                kilde = Kilde(
                    arbeidssoekerId = arbeidsoekerId,
                    identitetsnummer = identiteter.toSet()
                )
            )
            listOf(
                HendelseRecord(alias.first().recordKey, hendelse),
                HendelseRecord(idMap.recordKey, infoHendelse)
            )
        }

fun genererHendelse(metadata: Metadata, manuellIdOppdatering: ManuellIdOppdatering): List<HendelseRecord<Hendelse>> {
    return manuellIdOppdatering.lokaleAlias
        .distinctBy { it.arbeidsoekerId }
        .map { alias ->
        AutomatiskIdMergeIkkeMulig(
            identitetsnummer = alias.identitetsnummer,
            id = alias.arbeidsoekerId,
            hendelseId = UUID.randomUUID(),
            metadata = metadata,
            gjeldeneIdentitetsnummer = manuellIdOppdatering.gjeldeneIdentitetsnummer,
            pdlIdentitetsnummer = manuellIdOppdatering.pdlIdentitetsnummer,
            lokaleAlias = manuellIdOppdatering.lokaleAlias.map(::toDomeneAlias),
            perioder = manuellIdOppdatering.perioder.map(::tilDomenePerioderad)
        ).let { HendelseRecord(alias.recordKey, it) }
    }
}

fun tilDomenePerioderad(periodeRad: PeriodeRad): no.nav.paw.arbeidssokerregisteret.intern.v1.PeriodeRad {
    return no.nav.paw.arbeidssokerregisteret.intern.v1.PeriodeRad(
        periodeId = periodeRad.periodeId,
        identitetsnummer = periodeRad.identitetsnummer,
        fra = periodeRad.fra,
        til = periodeRad.til
    )
}

fun toDomeneAlias(alias: no.nav.paw.kafkakeygenerator.client.Alias): Alias {
    return Alias(
        identitetsnummer = alias.identitetsnummer,
        arbeidsoekerId = alias.arbeidsoekerId,
        recordKey = alias.recordKey,
        partition = alias.partition
    )
}
