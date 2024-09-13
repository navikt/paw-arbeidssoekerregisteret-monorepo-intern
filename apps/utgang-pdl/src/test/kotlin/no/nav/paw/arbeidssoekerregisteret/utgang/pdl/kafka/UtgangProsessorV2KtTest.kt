package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.application.*
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning

class UtgangProsessorV2KtTest : FreeSpec({
    "Under 18 år registrert via veilarb skal ikke trigge avslutning" {
        prosesser(
            InngangsReglerV3,
            inngangsOpplysninger = emptyList(),
            gjeldeneOpplysninger = listOf(
                DomeneOpplysning.ErUnder18Aar,
                DomeneOpplysning.BosattEtterFregLoven
            )
        ) shouldBe ProsesseringsResultat(
            grunnlag = setOf(ForhaandsgodkjentAvAnsatt),
            periodeSkalAvsluttes = false,
            forhaandsgodkjenningSkalSlettes = false
        )
    }

    "Under 18 år registert via veilarb, flyttet ut, Norsk statsborger skal trigge avslutning" {
        prosesser(
            InngangsReglerV3,
            inngangsOpplysninger = emptyList(),
            gjeldeneOpplysninger = listOf(
                DomeneOpplysning.ErUnder18Aar,
                DomeneOpplysning.IkkeBosatt
            )
        ) shouldBe ProsesseringsResultat(
            grunnlag = setOf(IkkeBosattINorgeIHenholdTilFolkeregisterloven),
            periodeSkalAvsluttes = true,
            forhaandsgodkjenningSkalSlettes = false
        )
    }

    "Under 18 år, EØS borger registrert via veilarb, ikke bosatt skal ikke trigge avslutning" {
        prosesser(
            InngangsReglerV3,
            inngangsOpplysninger = emptyList(),
            gjeldeneOpplysninger = listOf(
                DomeneOpplysning.ErUnder18Aar,
                DomeneOpplysning.IkkeBosatt,
                DomeneOpplysning.ErEuEoesStatsborger
            )
        ) shouldBe ProsesseringsResultat(
            grunnlag = setOf(ForhaandsgodkjentAvAnsatt),
            periodeSkalAvsluttes = false,
            forhaandsgodkjenningSkalSlettes = false
        )
    }

    "Over 18år, bosatt registert av registeret, ingen endringer, skal ikke trigge avslutning" {
        prosesser(
            InngangsReglerV3,
            inngangsOpplysninger = listOf(
                DomeneOpplysning.ErOver18Aar,
                DomeneOpplysning.BosattEtterFregLoven
            ),
            gjeldeneOpplysninger = listOf(
                DomeneOpplysning.ErOver18Aar,
                DomeneOpplysning.BosattEtterFregLoven
            )
        ) shouldBe ProsesseringsResultat(
            grunnlag = setOf(Over18AarOgBosattEtterFregLoven),
            periodeSkalAvsluttes = false,
            forhaandsgodkjenningSkalSlettes = false
        )
    }

    "Forhaandsgodkejent over 18år, ikke bosatt registert av registeret, ingen endringer, skal ikke trigge avslutning" {
        prosesser(
            InngangsReglerV3,
            inngangsOpplysninger = listOf(
                DomeneOpplysning.ErOver18Aar,
                DomeneOpplysning.IkkeBosatt,
                DomeneOpplysning.ErForhaandsgodkjent
            ),
            gjeldeneOpplysninger = listOf(
                DomeneOpplysning.ErOver18Aar,
                DomeneOpplysning.IkkeBosatt
            )
        ) shouldBe ProsesseringsResultat(
            grunnlag = setOf(ForhaandsgodkjentAvAnsatt),
            periodeSkalAvsluttes = false,
            forhaandsgodkjenningSkalSlettes = false
        )
    }

    "Forhaandsgodkejent over 18år, ikke bosatt registert av registeret, endres til bosatt, skal ikke trigge avslutning, men forhåndsgodkjenning skal slettes" {
        prosesser(
            InngangsReglerV3,
            inngangsOpplysninger = listOf(
                DomeneOpplysning.ErOver18Aar,
                DomeneOpplysning.IkkeBosatt,
                DomeneOpplysning.ErForhaandsgodkjent
            ),
            gjeldeneOpplysninger = listOf(
                DomeneOpplysning.ErOver18Aar,
                DomeneOpplysning.BosattEtterFregLoven
            )
        ) shouldBe ProsesseringsResultat(
            grunnlag = setOf(Over18AarOgBosattEtterFregLoven),
            periodeSkalAvsluttes = false,
            forhaandsgodkjenningSkalSlettes = true
        )
    }

    "Forhaandsgodkejent eøs borgere tremger ikke lenger forhåndsgodkjenning" {
        prosesser(
            InngangsReglerV3,
            inngangsOpplysninger = listOf(
                DomeneOpplysning.ErOver18Aar,
                DomeneOpplysning.ErForhaandsgodkjent
            ),
            gjeldeneOpplysninger = listOf(
                DomeneOpplysning.ErOver18Aar,
                DomeneOpplysning.IkkeBosatt,
                DomeneOpplysning.ErEuEoesStatsborger
            )
        ) shouldBe ProsesseringsResultat(
            grunnlag = setOf(EuEoesStatsborgerOver18Aar),
            periodeSkalAvsluttes = false,
            forhaandsgodkjenningSkalSlettes = true
        )
    }
})