package no.nav.paw.arbeidssokerregisteret.app.funksjoner

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.app.tilstand.*
import no.nav.paw.arbeidssokerregisteret.app.tilstand.GjeldeneTilstand.STARTET
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt

fun FunctionContext<TilstandV1?, Long>.opplysningerOmArbeidssoekerMottatt(hendelse: OpplysningerOmArbeidssoekerMottatt): InternTilstandOgApiTilstander =
    when {
        tilstand == null -> scope.harIngenTilstandLagreSomSisteOpplysninger(hendelse)
        tilstand.gjeldenePeriode == null -> FunctionContext(tilstand, scope).harAvsluttetPeriodeBareLagreSomSisteOpplysninger(hendelse)
        else -> FunctionContext(tilstand, scope).internTilstandOgApiTilstander(hendelse, tilstand.gjeldenePeriode)
    }.also { internTilstandOgApiTilstander ->
        Span.current().setAllAttributes(
            Attributes.of(
                AttributeKey.stringKey("paw.arbeidssoekerregisteret.hendelse.type"), hendelse.hendelseType,
                AttributeKey.stringKey("paw.arbeidssoekerregisteret.tilstand"), tilstand?.gjeldeneTilstand?.name ?: "null",
                AttributeKey.stringKey("paw.arbeidssoekerregisteret.opplysningerOmArbeidssoeker.skal_publiseres"), (internTilstandOgApiTilstander.nyOpplysningerOmArbeidssoekerTilstand != null).toString()
            )
        )
    }


private fun FunctionContext<TilstandV1, Long>.internTilstandOgApiTilstander(
    hendelse: OpplysningerOmArbeidssoekerMottatt,
    gjeldenePeriode: Periode
) = InternTilstandOgApiTilstander(
    id = scope.id,
    tilstand = tilstand.copy(
        sisteOpplysningerOmArbeidssoeker = hendelse.opplysningerOmArbeidssoeker,
        forrigeOpplysningerOmArbeidssoeker = tilstand.sisteOpplysningerOmArbeidssoeker,
        hendelseScope = scope
    ),
    nyOpplysningerOmArbeidssoekerTilstand = if (tilstand.gjeldeneTilstand == STARTET) {
        OpplysningerOmArbeidssoeker(
            hendelse.opplysningerOmArbeidssoeker.id,
            gjeldenePeriode.id,
            hendelse.metadata.api(),
            hendelse.opplysningerOmArbeidssoeker.utdanning?.api(),
            hendelse.opplysningerOmArbeidssoeker.helse?.api(),
            hendelse.opplysningerOmArbeidssoeker.jobbsituasjon.api(),
            hendelse.opplysningerOmArbeidssoeker.annet?.api()
        )
    } else null,
    nyPeriodeTilstand = null
)

private fun FunctionContext<TilstandV1, Long>.harAvsluttetPeriodeBareLagreSomSisteOpplysninger(
    hendelse: OpplysningerOmArbeidssoekerMottatt
) = InternTilstandOgApiTilstander(
    id = scope.id,
    tilstand = tilstand.copy(
        sisteOpplysningerOmArbeidssoeker = hendelse.opplysningerOmArbeidssoeker,
        forrigeOpplysningerOmArbeidssoeker = tilstand.sisteOpplysningerOmArbeidssoeker,
        gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
        alleIdentitetsnummer = tilstand.alleIdentitetsnummer + hendelse.identitetsnummer,
        hendelseScope = scope
    ),
    nyOpplysningerOmArbeidssoekerTilstand = null,
    nyPeriodeTilstand = null
)

private fun HendelseScope<Long>.harIngenTilstandLagreSomSisteOpplysninger(
    hendelse: OpplysningerOmArbeidssoekerMottatt
) = InternTilstandOgApiTilstander(
    id = id,
    tilstand = TilstandV1(
        hendelseScope = this,
        gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
        alleIdentitetsnummer = setOf(hendelse.identitetsnummer),
        gjeldeneTilstand = GjeldeneTilstand.AVSLUTTET,
        gjeldenePeriode = null,
        forrigePeriode = null,
        sisteOpplysningerOmArbeidssoeker = hendelse.opplysningerOmArbeidssoeker,
        forrigeOpplysningerOmArbeidssoeker = null
    ),
    nyOpplysningerOmArbeidssoekerTilstand = null,
    nyPeriodeTilstand = null
)
