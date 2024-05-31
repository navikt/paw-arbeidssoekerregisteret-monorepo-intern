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

context(HendelseScope<Long>)
@WithSpan(
    value = "opplysningerOmArbeidssoekerMottatt",
    kind = SpanKind.INTERNAL
)
fun TilstandV1?.opplysningerOmArbeidssoekerMottatt(hendelse: OpplysningerOmArbeidssoekerMottatt): InternTilstandOgApiTilstander =
    when {
        this == null -> harIngenTilstandLagreSomSisteOpplysninger(hendelse)
        this.gjeldenePeriode == null -> harAvsluttetPeriodeBareLagreSomSisteOpplysninger(hendelse)
        else -> internTilstandOgApiTilstander(hendelse, gjeldenePeriode)
    }.also { internTilstandOgApiTilstander ->
        Span.current().setAllAttributes(
            Attributes.of(
                AttributeKey.stringKey("paw.arbeidssoekerregisteret.hendelse.type"), hendelse.hendelseType,
                AttributeKey.stringKey("paw.arbeidssoekerregisteret.tilstand"), this?.gjeldeneTilstand?.name ?: "null",
                AttributeKey.stringKey("paw.arbeidssoekerregisteret.opplysningerOmArbeidssoeker.skal_publiseres"), (internTilstandOgApiTilstander.nyOpplysningerOmArbeidssoekerTilstand != null).toString()
            )
        )
    }


context(HendelseScope<Long>)
private fun TilstandV1.internTilstandOgApiTilstander(
    hendelse: OpplysningerOmArbeidssoekerMottatt,
    gjeldenePeriode: Periode
) = InternTilstandOgApiTilstander(
    id = id,
    tilstand = this.copy(
        sisteOpplysningerOmArbeidssoeker = hendelse.opplysningerOmArbeidssoeker,
        forrigeOpplysningerOmArbeidssoeker = this.sisteOpplysningerOmArbeidssoeker,
        hendelseScope = currentScope()
    ),
    nyOpplysningerOmArbeidssoekerTilstand = if (gjeldeneTilstand == STARTET) {
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

context(HendelseScope<Long>)
private fun TilstandV1.harAvsluttetPeriodeBareLagreSomSisteOpplysninger(
    hendelse: OpplysningerOmArbeidssoekerMottatt
) = InternTilstandOgApiTilstander(
    id = id,
    tilstand = this.copy(
        sisteOpplysningerOmArbeidssoeker = hendelse.opplysningerOmArbeidssoeker,
        forrigeOpplysningerOmArbeidssoeker = this.sisteOpplysningerOmArbeidssoeker,
        gjeldeneIdentitetsnummer = hendelse.identitetsnummer,
        alleIdentitetsnummer = this.alleIdentitetsnummer + hendelse.identitetsnummer,
        hendelseScope = currentScope()
    ),
    nyOpplysningerOmArbeidssoekerTilstand = null,
    nyPeriodeTilstand = null
)

private fun HendelseScope<Long>.harIngenTilstandLagreSomSisteOpplysninger(
    hendelse: OpplysningerOmArbeidssoekerMottatt
) = InternTilstandOgApiTilstander(
    id = id,
    tilstand = TilstandV1(
        hendelseScope = currentScope(),
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
