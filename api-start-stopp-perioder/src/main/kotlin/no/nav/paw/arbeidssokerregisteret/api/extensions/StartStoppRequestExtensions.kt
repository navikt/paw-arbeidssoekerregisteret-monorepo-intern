package no.nav.paw.arbeidssokerregisteret.api.extensions

import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV1ArbeidssokerPeriodePutRequest
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.tilIdentitetsnummer

fun ApiV1ArbeidssokerPeriodePutRequest.getId(): Identitetsnummer = identitetsnummer.tilIdentitetsnummer()
