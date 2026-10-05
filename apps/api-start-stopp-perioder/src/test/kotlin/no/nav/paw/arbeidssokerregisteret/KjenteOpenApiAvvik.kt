package no.nav.paw.arbeidssokerregisteret

import no.nav.paw.test.openapi.KjentAvvik

/**
 * Avvik mellom svar og OpenAPI-spesifikasjon som fantes før valideringen ble innført.
 * Avvikene ignoreres slik at testene er grønne, men nye avvik feiler.
 * Fjern linjen når koden eller spesifikasjonen er rettet.
 */
val kjenteOpenApiAvvik: List<KjentAvvik> = listOf(
    KjentAvvik(
        metode = "PUT",
        sti = "/api/v2/arbeidssoker/periode",
        status = 204,
        noekkel = "validation.response.body.missing",
        begrunnelse = "Spesifikasjonen har body på 204, appen svarer uten body"
    ),
    KjentAvvik(
        metode = "POST",
        sti = "/api/v1/arbeidssoker/opplysninger",
        status = 202,
        noekkel = "validation.response.body.missing",
        begrunnelse = "Spesifikasjonen har body på 202, appen svarer uten body"
    ),
    KjentAvvik(
        metode = "PUT",
        sti = "/api/v2/arbeidssoker/periode",
        status = 400,
        noekkel = "validation.response.body.schema.type",
        begrunnelse = "Appen svarer med null i stedet for feilobjekt"
    ),
    KjentAvvik(
        metode = "PUT",
        sti = "/api/v2/arbeidssoker/periode",
        status = 403,
        noekkel = "validation.response.body.schema.type",
        begrunnelse = "Appen svarer med null i stedet for feilobjekt"
    ),
    KjentAvvik(
        metode = "PUT",
        sti = "/api/v2/arbeidssoker/periode",
        status = 403,
        noekkel = "validation.response.body.schema.required",
        begrunnelse = "Feilsvaret mangler påkrevde felt 'beskrivelse' og 'regel'"
    )
)
