package no.nav.paw.arbeidssokerregisteret.evaluering.regler.retttilregistrering

import no.nav.paw.arbeidssokerregisteret.evaluering.Fakta
import no.nav.paw.arbeidssokerregisteret.evaluering.regler.Regel
import no.nav.paw.arbeidssokerregisteret.evaluering.regler.invoke

val harRettTilRegistrering: List<Regel> = listOf(
    "Er forhåndsgodkjent av ansatt"(
        Fakta.FORHAANDSGODKJENT_AV_ANSATT
    ),
    "Er over 18 år, har norsk adresse og oppholdstillatelse"(
        Fakta.ER_OVER_18_AAR,
        Fakta.HAR_NORSK_ADRESSE,
        Fakta.HAR_GYLDIG_OPPHOLDSTILLATELSE,
    ),
    "Er over 18 år, har norsk adresse og er bosatt i Norge etter Folkeregisterloven"(
        Fakta.ER_OVER_18_AAR,
        Fakta.HAR_NORSK_ADRESSE,
        Fakta.BOSATT_ETTER_FREG_LOVEN
    ),
    "Er over 18 år, har norsk adresse og har d-nummer"(
        Fakta.ER_OVER_18_AAR,
        Fakta.HAR_NORSK_ADRESSE,
        Fakta.DNUMMER
    )
)
