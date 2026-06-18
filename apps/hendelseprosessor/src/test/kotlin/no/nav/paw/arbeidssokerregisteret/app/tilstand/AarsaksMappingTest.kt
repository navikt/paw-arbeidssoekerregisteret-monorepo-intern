package no.nav.paw.arbeidssokerregisteret.app.tilstand

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Aarsaksinformasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvsluttetAarsakType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.RegelEvalResultat
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType as ApiAvsluttetAarsakType

class AarsaksMappingTest : FreeSpec({

    "Aarsaksinformasjon.api() skal mappe alle interne årsakstyper til riktig API-type" - {

        "SVARTE_NEI_I_BEKREFTELSE skal mappes til SVARTE_NEI_I_BEKREFTELSE" {
            val aarsak = Aarsaksinformasjon(
                aarsak = AvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE,
                regelEvalResultat = RegelEvalResultat.IKKE_RELEVANT
            )
            val result = aarsak.api()
            result.shouldNotBeNull()
            result.aarsaksinformasjon.type shouldBe ApiAvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE
        }

        "BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST skal mappes til BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST" {
            val aarsak = Aarsaksinformasjon(
                aarsak = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST,
                regelEvalResultat = RegelEvalResultat.IKKE_RELEVANT
            )
            val result = aarsak.api()
            result.shouldNotBeNull()
            result.aarsaksinformasjon.type shouldBe ApiAvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
        }

        "FEILREGISTRERING skal mappes til UDEFINERT (perioden skal ikke eksponeres som feilregistrering eksternt)" {
            val aarsak = Aarsaksinformasjon(
                aarsak = AvsluttetAarsakType.FEILREGISTRERING,
                regelEvalResultat = RegelEvalResultat.IKKE_RELEVANT
            )
            val result = aarsak.api()
            result.shouldNotBeNull()
            result.aarsaksinformasjon.type shouldBe ApiAvsluttetAarsakType.UDEFINERT
        }

        "UDEFINERT skal mappes til UDEFINERT" {
            val aarsak = Aarsaksinformasjon(
                aarsak = AvsluttetAarsakType.UDEFINERT,
                regelEvalResultat = RegelEvalResultat.IKKE_RELEVANT
            )
            val result = aarsak.api()
            result.shouldNotBeNull()
            result.aarsaksinformasjon.type shouldBe ApiAvsluttetAarsakType.UDEFINERT
        }

        "UKJENT_VERDI skal mappes til UKJENT_VERDI" {
            val aarsak = Aarsaksinformasjon(
                aarsak = AvsluttetAarsakType.UKJENT_VERDI,
                regelEvalResultat = RegelEvalResultat.UKJENT_VERDI
            )
            val result = aarsak.api()
            result.shouldNotBeNull()
            result.aarsaksinformasjon.type shouldBe ApiAvsluttetAarsakType.UKJENT_VERDI
        }
    }

    "Null aarsaksInformasjon skal gi null avslutningsInfo i Periode" {
        val aarsaksInformasjon: Aarsaksinformasjon? = null
        aarsaksInformasjon?.api().shouldBeNull()
    }
})
