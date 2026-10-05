package no.nav.paw.kafkakeygenerator.context

import no.nav.paw.test.openapi.KjentAvvik

/**
 * Avvik mellom svar og OpenAPI-spesifikasjon som fantes før valideringen ble innført.
 * Avvikene ignoreres slik at testene er grønne, men nye avvik feiler.
 * Fjern linjen når koden eller spesifikasjonen er rettet.
 */
val kjenteOpenApiAvvik: List<KjentAvvik> = listOf(
    KjentAvvik(
        metode = "POST",
        sti = "/api/v1/record-key",
        status = 404,
        noekkel = "validation.response.status.unknown",
        begrunnelse = "404 er ikke dokumentert"
    )
)
