package no.nav.paw.arbeidssokerregisteret.api.extensions

import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV2ArbeidssokerKanStartePeriodePutRequest
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.tilIdentitetsnummer

fun ApiV2ArbeidssokerKanStartePeriodePutRequest.getId(): Identitetsnummer = identitetsnummer.tilIdentitetsnummer()
