package no.nav.paw.arbeidssokerregisteret.domain.http

import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.tilIdentitetsnummer
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Annet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Arbeidserfaring
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Helse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Utdanning

data class OpplysningerRequest(
    val identitetsnummer: String,
    val opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoekerRequest
) {
    fun getId(): Identitetsnummer = identitetsnummer.tilIdentitetsnummer()
}

data class OpplysningerOmArbeidssoekerRequest(
    val utdanning: Utdanning,
    val helse: Helse,
    val jobbsituasjon: Jobbsituasjon,
    val arbeidserfaring: Arbeidserfaring,
    val annet: Annet
)
