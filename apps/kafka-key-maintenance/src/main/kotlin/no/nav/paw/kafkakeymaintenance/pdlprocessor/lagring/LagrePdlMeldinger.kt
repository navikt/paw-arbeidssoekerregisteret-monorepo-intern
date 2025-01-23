package no.nav.paw.kafkakeymaintenance.pdlprocessor.lagring

import io.opentelemetry.api.trace.Span
import no.nav.paw.kafkakeymaintenance.kafka.HwmRunnerProcessor
import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import no.nav.paw.kafkakeymaintenance.pdlprocessor.tilIdentRader
import no.nav.person.pdl.aktor.v2.Aktor
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import java.time.Instant

private val lagreAktorLogger = LoggerFactory.getLogger("lagreAktorMelding")

class LagreAktorMelding : HwmRunnerProcessor<String, Aktor> {

    override fun process(txContext: TransactionContext, record: ConsumerRecord<String, Aktor>) {
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
            txContext.settInEllerOppdatere(
                record.key(),
                tidspunktFraKilde = Instant.ofEpochMilli(record.timestamp()),
                tidspunkt = Instant.now(),
                aktor = record.value(),
                traceparent = traceparent
            )
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
    traceparent: String
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
    return SetInEllerOpdaterResultat(
        person = person,
        forrigeTilstand = lagredeIdenter,
        nyTilstand = identerFraPdl
    )
}
