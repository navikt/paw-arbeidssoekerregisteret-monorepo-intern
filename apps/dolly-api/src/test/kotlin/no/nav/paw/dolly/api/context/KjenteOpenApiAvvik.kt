package no.nav.paw.dolly.api.context

import no.nav.paw.test.openapi.KjentAvvik

/**
 * Avvik mellom svar og OpenAPI-spesifikasjon som fantes før valideringen ble innført.
 * Avvikene ignoreres slik at testene er grønne, men nye avvik feiler.
 * Fjern linjen når koden eller spesifikasjonen er rettet.
 */
val kjenteOpenApiAvvik: List<KjentAvvik> = listOf(
    KjentAvvik(
        metode = "POST",
        sti = "/api/v1/arbeidssoekerregistrering",
        status = 403,
        noekkel = "validation.response.status.unknown",
        begrunnelse = "403 er ikke dokumentert"
    ),
    KjentAvvik(
        metode = "GET",
        sti = "/api/v1/arbeidssoekerregistrering/[^/]+",
        status = 403,
        noekkel = "validation.response.status.unknown",
        begrunnelse = "403 er ikke dokumentert"
    ),
    KjentAvvik(
        metode = "DELETE",
        sti = "/api/v1/arbeidssoekerregistrering/[^/]+",
        status = 403,
        noekkel = "validation.response.status.unknown",
        begrunnelse = "403 er ikke dokumentert"
    ),
    KjentAvvik(
        metode = "GET",
        sti = "/api/v1/typer/[^/]+",
        status = 403,
        noekkel = "validation.response.status.unknown",
        begrunnelse = "403 er ikke dokumentert"
    )
)
