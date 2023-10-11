package no.nav.paw.arbeidssokerregisteret.services

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.domain.Foedselsnummer
import no.nav.paw.arbeidssokerregisteret.domain.harOppholdstillatelse
import no.nav.paw.arbeidssokerregisteret.intern.StartV1
import no.nav.paw.arbeidssokerregisteret.kafka.producers.ArbeidssokerperiodeStartProducer
import no.nav.paw.arbeidssokerregisteret.plugins.StatusException
import no.nav.paw.arbeidssokerregisteret.utils.logger
import no.nav.paw.pdl.PdlClient
import no.nav.paw.pdl.hentOpphold
import org.slf4j.MDC
import java.time.Instant

class ArbeidssokerService(
    private val pdlClient: PdlClient,
    private val arbeidssokerperiodeStartProducer: ArbeidssokerperiodeStartProducer
) {
    fun startArbeidssokerperiode(foedselsnummer: Foedselsnummer, opprettetAv: String) {
        val harOpphold = runBlocking {
            pdlClient.hentOpphold(foedselsnummer.verdi, MDC.get("x_callId"), "paw-arbeidssokerregisteret")
        }
            .harOppholdstillatelse()

        if (!harOpphold) {
            logger.info("Bruker har ikke oppholdstillatelse, starter ikke arbeidssokerperiode")
            throw StatusException(HttpStatusCode.Forbidden, "Bruker har ikke oppholdstillatelse")
        }

        arbeidssokerperiodeStartProducer.publish(StartV1(foedselsnummer.verdi, Instant.now(), opprettetAv, "kilde"))
    }

    fun avsluttArbeidssokerperiode(foedselsnummer: Foedselsnummer, opprettetAv: String) {
        TODO()
    }

    fun kanRegistreresSomArbeidssoker(foedselsnummer: Foedselsnummer): Boolean {
        val harOpphold = runBlocking {
            pdlClient.hentOpphold(foedselsnummer.verdi, MDC.get("x_callId"), "paw-arbeidssokerregisteret")
        }
            .harOppholdstillatelse()

        if (!harOpphold) {
            logger.info("Bruker har ikke oppholdstillatelse, starter ikke arbeidssokerperiode")
            // TODO: Mer informasjon om hvorfor bruker ikke kan registrere seg som arbeidssøker
            throw StatusException(HttpStatusCode.Forbidden, "Bruker har ikke oppholdstillatelse")
        }

        return true
    }
}
