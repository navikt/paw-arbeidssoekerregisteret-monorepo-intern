package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning.*
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning

/**
 * Oppsummering av V2 regler:
 * - IkkeFunnet, Doed, Savnet og Opphoert skal alltid avvises, kan ikke overstyres.
 * - Alle over 18 år som er bosatt etter folkeregisterloven skal godkjennes.
 * - Borgere som ikke er Norske men fra et annet EU/EØS land og er  over 18 år og uten IKKE_BOSATT (utflyttet) skal godkjennes.
 * - Alle som ikke treffer en av de 2 regelene for godkjenning vil få treff i minst en "muligGrunnlagForAvvisning" regel.
 */
object InngangsReglerV2 : Regler {
    override val regler: List<Regel> = listOf(
        IkkeFunnet(
            PersonIkkeFunnet,
            vedTreff = ::skalAvises
        ),
        Doed(
            ErDoed,
            vedTreff = ::skalAvises
        ),
        Savnet(
            ErSavnet,
            vedTreff = ::skalAvises
        ),
        Opphoert(
            OpphoertIdentitet,
            vedTreff = ::skalAvises
        ),
        ForhaandsgodkjentAvAnsatt(
            ErForhaandsgodkjent,
            vedTreff = ::grunnlagForGodkjenning
        ),
        Under18Aar(
            ErUnder18Aar,
            vedTreff = ::muligGrunnlagForAvvisning
        ),
        UkjentAlder(
            UkjentFoedselsaar,
            UkjentFoedselsdato,
            vedTreff = ::muligGrunnlagForAvvisning
        ),
        Over18AarOgBosattEtterFregLoven(
            ErOver18Aar,
            BosattEtterFregLoven,
            vedTreff = ::grunnlagForGodkjenning
        ),
        EuEoesStatsborgerOver18Aar(
            ErOver18Aar,
            ErEuEoesStatsborger,
            !ErNorskStatsborger,
            !IkkeBosatt,
            vedTreff = ::grunnlagForGodkjenning
        ),
        EuEoesStatsborgerMenHarStatusIkkeBosatt(
            ErEuEoesStatsborger,
            !ErNorskStatsborger,
            IkkeBosatt,
            vedTreff = ::muligGrunnlagForAvvisning
        ),
        IkkeBosattINorgeIHenholdTilFolkeregisterloven(
            !BosattEtterFregLoven,
            ErNorskEllerTredjelandsborger,
            vedTreff = ::muligGrunnlagForAvvisning
        )
    )

    override val standardRegel: Regel = IkkeBosattINorgeIHenholdTilFolkeregisterloven(
        vedTreff = ::muligGrunnlagForAvvisning
    )
}

data object ErNorskEllerTredjelandsborger : Condition {
    override fun eval(opplysninger: Iterable<Opplysning>): Boolean =
        ErNorskStatsborger in opplysninger || (ErEuEoesStatsborger !in opplysninger)
}

