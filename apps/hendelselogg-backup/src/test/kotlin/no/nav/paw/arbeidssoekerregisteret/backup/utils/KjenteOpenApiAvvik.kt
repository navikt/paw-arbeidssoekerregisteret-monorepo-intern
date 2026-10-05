package no.nav.paw.arbeidssoekerregisteret.backup.utils

import no.nav.paw.test.openapi.KjentAvvik

/**
 * Avvik mellom svar og OpenAPI-spesifikasjon som fantes før valideringen ble innført.
 * Avvikene ignoreres slik at testene er grønne, men nye avvik feiler.
 * Fjern linjen når koden eller spesifikasjonen er rettet.
 */
val kjenteOpenApiAvvik: List<KjentAvvik> = listOf(
    KjentAvvik(
        metode = "POST",
        sti = "/api/v1/arbeidssoeker/detaljer",
        status = 200,
        noekkel = "validation.response.body.schema.required",
        begrunnelse = "Skjemaet krever 'arbeidssoekId', trolig skrivefeil i spesifikasjonen"
    ),
    KjentAvvik(
        metode = "POST",
        sti = "/api/v1/arbeidssoeker/detaljer",
        status = 400,
        noekkel = "validation.response.body.schema.additionalProperties",
        begrunnelse = "Feilsvaret har 'timestamp', som ikke står i skjemaet"
    ),
    KjentAvvik(
        metode = "POST",
        sti = "/api/v1/arbeidssoeker/detaljer",
        status = 404,
        noekkel = "validation.response.status.unknown",
        begrunnelse = "404 er ikke dokumentert"
    )
)
