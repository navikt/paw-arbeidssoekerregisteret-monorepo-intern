package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import io.opentelemetry.api.trace.Span
import no.nav.paw.kafkakeymaintenance.SecureLogger
import no.nav.paw.kafkakeymaintenance.kafka.HwmRunnerProcessor
import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import no.nav.paw.kafkakeymaintenance.pdlprocessor.tilIdentRader
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.time.Instant

private val lagreAktorLogger = LoggerFactory.getLogger("lagreAktorMelding")

class LagreAktorMelding(
    private val startDatoForMergeProsessering: Instant
) : HwmRunnerProcessor<String, Aktor> {

    override fun process(txContext: TransactionContext, record: ConsumerRecord<String, Aktor>) {
        if (record.offset() == 3829596L) {
            lagreAktorLogger.info("Ignorerer melding: offset={}", record.offset())
            return
        }
        runCatching {
            if (record.value() == null) {
                lagreAktorLogger.info(
                    "Sletter aktør: null={}",
                    record.value() == null
                )
                txContext.slettPerson(record.key())
            } else {
                val traceparent = Span.current().spanContext.let { ctx ->
                    "00-${ctx.traceId}-${ctx.spanId}-${ctx.traceFlags.asHex()}"
                }
                val tidspunktFraKilde = Instant.ofEpochMilli(record.timestamp())
                txContext.settInEllerOppdatere(
                    record.key(),
                    tidspunktFraKilde = tidspunktFraKilde,
                    tidspunkt = Instant.now(),
                    aktor = record.value(),
                    traceparent = traceparent,
                    mergeProsessert = tidspunktFraKilde.isBefore(startDatoForMergeProsessering)
                )
            }
        }.onFailure { error ->
            SecureLogger.error(
                "Feil ved lagring av aktør, melding: '{}'",
                record.value(),
                error
            )
        }.getOrElse {
            throw Exception("Feilet under prosessering av melding(${record.partition()}::${record.offset()}), se securelogs for detaljer")
        }
    }

    override fun ignore(record: ConsumerRecord<String, Aktor>): Boolean {
        return false
    }
}


class SetInEllerOpdaterResultat(
    val person: Person,
    val forrigeTilstand: List<Ident>,
    val nyTilstand: List<Ident>
)

fun TransactionContext.settInEllerOppdatere(
    recordKey: String,
    tidspunktFraKilde: Instant,
    tidspunkt: Instant,
    aktor: Aktor,
    traceparent: String,
    mergeProsessert: Boolean
): SetInEllerOpdaterResultat {
    val (person, personOpprettet) = (hentPerson(recordKey)?.let { it to false })
        ?: (opprettPerson(
            recordKey = recordKey,
            tidspunktFraKilde = tidspunktFraKilde,
            tidspunkt = tidspunkt,
            traceId = traceparent
        ) to true)
    val lagredeIdenter = hentIdenter(person.personId)
    val identerFraPdl = tilIdentRader(aktor)
    val diffResultat = diffAtilB(lagredeIdenter, identerFraPdl)
    diffResultat.slettet.forEach {
        slettIdent(
            personId = person.personId,
            ident = it.ident,
            identType = it.identType
        )
    }
    diffResultat.opprettet.forEach {
        opprettIdent(
            personId = person.personId,
            ident = it.ident,
            identType = it.identType,
            gjeldende = it.gjeldende
        )
    }
    if (!personOpprettet) {
        oppdaterPerson(
            personId = person.personId,
            tidspunkt = tidspunkt,
            tidspunktFraKilde = tidspunktFraKilde,
            traceId = traceparent
        )
    }
    if (mergeProsessert) {
        mergeProsessert(personId = person.personId)
    }
    return SetInEllerOpdaterResultat(
        person = person,
        forrigeTilstand = lagredeIdenter,
        nyTilstand = identerFraPdl
    )
}
