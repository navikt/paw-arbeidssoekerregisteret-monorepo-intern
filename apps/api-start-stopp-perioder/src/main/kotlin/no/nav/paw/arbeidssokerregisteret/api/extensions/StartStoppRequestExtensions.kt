package no.nav.paw.arbeidssokerregisteret.api.extensions

import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV2ArbeidssokerPeriodePutRequest
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.felles.model.asIdentitetsnummer

fun ApiV2ArbeidssokerPeriodePutRequest.getId(): Identitetsnummer = identitetsnummer.asIdentitetsnummer()
