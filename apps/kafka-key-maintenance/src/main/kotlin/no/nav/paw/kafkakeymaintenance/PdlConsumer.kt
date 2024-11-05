package no.nav.paw.kafkakeymaintenance

import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerOpphoert
import no.nav.paw.arbeidssokerregisteret.intern.v1.IdentitetsnummerSammenslaatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.LokaleAlias
import no.nav.paw.kafkakeymaintenance.functions.genererIdOppdatering
import no.nav.paw.kafkakeymaintenance.functions.harAvvik
import no.nav.paw.kafkakeymaintenance.functions.hentData
import no.nav.paw.kafkakeymaintenance.functions.hentPerioder
import no.nav.paw.kafkakeymaintenance.kafka.Topic
import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import no.nav.paw.kafkakeymaintenance.kafka.updateHwm
import no.nav.paw.kafkakeymaintenance.vo.*
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*

const val ANTALL_PARTISJONER = 6

fun KafkaKeysClient.hentAlias(identiteter: List<String>): List<LokaleAlias> = runBlocking {
    getAlias(ANTALL_PARTISJONER, identiteter).alias
}

fun process(
    txContextFactory: Transaction.() -> TransactionContext,
    hentAlias: (List<String>) -> List<LokaleAlias>,
    record: ConsumerRecord<String, Aktor>
): IdOppdatering? =
    transaction {
        val txContext = txContextFactory()
        val valid = txContext.updateHwm(
            topic = Topic(record.topic()),
            partition = record.partition(),
            offset = record.offset(),
            time = Instant.ofEpochMilli(record.timestamp()),
            lastUpdated = Instant.now()
        )
        if (valid) {
            hentData(hentAlias, record)
                .takeIf(::harAvvik)
                ?.let(::avviksMelding)
                ?.let(txContext::hentPerioder)
                ?.let(::genererIdOppdatering)
        } else {
            null
        }
    }

fun genererHendelser(metadata: Metadata, idOppdatering: IdOppdatering): List<Hendelse> {
    return when (idOppdatering) {
        is AutomatiskIdOppdatering -> genererHendelse(metadata, idOppdatering)
        is ManuellIdOppdatering -> TODO()
    }
}

fun genererHendelse(metadata: Metadata, idOppdatering: AutomatiskIdOppdatering): List<Hendelse> {
    val identitetsnummerOpphoert = idOppdatering
        .frieIdentiteter
        .groupBy { it.arbeidsoekerId }
        .map { (arbeidsoekerId, alias) ->
            val identiteter = alias.map { it.identitetsnummer }
            IdentitetsnummerOpphoert(
                id = arbeidsoekerId,
                hendelseId = UUID.randomUUID(),
                identitetsnummer = identiteter.first(),
                metadata = metadata,
                alleIdentitetsnummer = identiteter
            )
        }
    val identitetsnummerSammenslaatt = idOppdatering.oppdatertData?.let { genererHendelse(metadata, it) } ?: emptyList()
    TODO()
}

fun genererHendelse(metadata: Metadata, idMap: IdMap): List<IdentitetsnummerSammenslaatt> =
   idMap.identiteter
       .filter { it.arbeidsoekerId != idMap.arbeidsoekerId }
       .groupBy { it.arbeidsoekerId }
       .map { (arbeidsoekerId, alias) ->
           val identiteter = alias.map { it.identitetsnummer }
           IdentitetsnummerSammenslaatt(
               id = arbeidsoekerId,
               hendelseId = UUID.randomUUID(),
               identitetsnummer = identiteter.first(),
               metadata = metadata,
               alleIdentitetsnummer = identiteter,
               flyttetTilArbeidssoekerId = idMap.arbeidsoekerId
           )
       }

