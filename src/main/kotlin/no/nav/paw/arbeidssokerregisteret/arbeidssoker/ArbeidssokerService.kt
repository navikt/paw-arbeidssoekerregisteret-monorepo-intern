package no.nav.paw.arbeidssokerregisteret.arbeidssoker

import no.nav.paw.arbeidssokerregisteret.arbeidssoker.domain.ArbeidssokerPerioder
import no.nav.paw.arbeidssokerregisteret.arbeidssoker.domain.Foedselsnummer
import no.nav.paw.arbeidssokerregisteret.utils.logger
import java.time.LocalDateTime

class ArbeidssokerService(
    private val arbeidssokerRepository: ArbeidssokerRepository,
) {
    fun startPeriode(foedselsnummer: Foedselsnummer, fraOgMedDato: LocalDateTime = LocalDateTime.now()) {
        val sistePeriode = arbeidssokerRepository.hentSistePeriode(foedselsnummer)

        if (sistePeriode?.aktiv == true) {
            throw Exception("Kan ikke starte ny periode fordi arbeidssøker allerede har en aktiv periode")
        }

        arbeidssokerRepository.opprett(foedselsnummer, fraOgMedDato)
    }

    fun avsluttPeriode(foedselsnummer: Foedselsnummer, tilOgMedDato: LocalDateTime = LocalDateTime.now()) {
        val gjelendePeriode = arbeidssokerRepository.hentSistePeriode(foedselsnummer)

        if (gjelendePeriode == null || !gjelendePeriode.aktiv) {
            throw Exception("Kan ikke avslutte periode fordi arbeidssøker ikke har en aktiv periode")
        }

        arbeidssokerRepository.avslutt(gjelendePeriode.id, tilOgMedDato)
        logger.info("Avsluttet periode for arbeidssøker")
    }

    fun hentPerioder(foedselsnummer: Foedselsnummer): ArbeidssokerPerioder = arbeidssokerRepository.hent(foedselsnummer)
}
