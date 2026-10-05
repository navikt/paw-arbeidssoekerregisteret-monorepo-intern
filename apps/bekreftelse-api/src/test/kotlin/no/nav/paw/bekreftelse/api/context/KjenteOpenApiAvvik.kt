package no.nav.paw.bekreftelse.api.context

import no.nav.paw.test.openapi.KjentAvvik

/**
 * Avvik mellom svar og OpenAPI-spesifikasjon som fantes før valideringen ble innført.
 * Avvikene ignoreres slik at testene er grønne, men nye avvik feiler.
 * Fjern linjen når koden eller spesifikasjonen er rettet.
 */
val kjenteOpenApiAvvik: List<KjentAvvik> = listOf(
    KjentAvvik(
        metode = "GET",
        sti = "/api/v1/tilgjengelige-bekreftelser",
        status = 403,
        noekkel = "validation.response.body.missing",
        begrunnelse = "Spesifikasjonen har body på 403, noen 403-svar er uten body"
    ),
    KjentAvvik(
        metode = "GET",
        sti = "/api/v1/tilgjengelige-bekreftelser",
        status = 403,
        noekkel = "validation.response.body.schema.additionalProperties",
        begrunnelse = "Feilsvaret har 'timestamp', som ikke står i skjemaet"
    ),
    KjentAvvik(
        metode = "POST",
        sti = "/api/v1/tilgjengelige-bekreftelser",
        status = 403,
        noekkel = "validation.response.body.schema.additionalProperties",
        begrunnelse = "Feilsvaret har 'timestamp', som ikke står i skjemaet"
    ),
    KjentAvvik(
        metode = "POST",
        sti = "/api/v1/bekreftelse",
        status = 400,
        noekkel = "validation.response.body.schema.additionalProperties",
        begrunnelse = "Feilsvaret har 'timestamp', som ikke står i skjemaet"
    ),
    KjentAvvik(
        metode = "POST",
        sti = "/api/v1/bekreftelse",
        status = 403,
        noekkel = "validation.response.body.schema.additionalProperties",
        begrunnelse = "Feilsvaret har 'timestamp', som ikke står i skjemaet"
    )
)
