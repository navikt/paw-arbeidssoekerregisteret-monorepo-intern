# Dolly API (DEV)

API for registrering av en person som arbeidssøker i arbeidssøkerregisteret ved opprettelsen av testpersoner i Dolly.

## Dokumentasjon for API

https://dolly-arbeidssoekerregisteret.intern.dev.nav.no/docs

## Default-verdier

```
  utfoertAv = BrukerType.SLUTTBRUKER,
  kilde = "Dolly",
  aarsak = "Registrering av arbeidssøker i Dolly",
  nuskode = "3",
  utdanningBestaatt = true,
  utdanningGodkjent = true,
  jobbsituasjonBeskrivelse = Beskrivelse.HAR_BLITT_SAGT_OPP,
  jobbsituasjonDetaljer = Detaljer(stillingStyrk08 = "00", stilling = "Annen stilling"),
  helsetilstandHindrerArbeid = false,
  andreForholdHindrerArbeid = false
```

For alle nuskoder under "3" blir utdanningBestaatt og utdanningGodkjent satt til null.