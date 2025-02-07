# Dolly API (DEV)

API for registrering av en person som arbeidssøker i arbeidssøkerregisteret ved opprettelsen av testpersoner i Dolly.

## Dokumentasjon for API

https://dolly-arbeidssoekerregisteret.intern.dev.nav.no/docs

## Default-verdier

Felt som ikke er satt i request bruker følgende default-verdier.
```
  utfoertAv = Brukertype.SLUTTBRUKER,
  kilde = "Dolly",
  aarsak = "Registrering av arbeidssøker i Dolly",
  nuskode = "3",
  utdanningBestaatt = true,
  utdanningGodkjent = true,
  jobbsituasjonsbeskrivelse = Jobbsituasjonsbeskrivelse.HAR_BLITT_SAGT_OPP,
  jobbsituasjonsdetaljer = Jobbsituasjonsdetaljer(stillingStyrk08 = "00", stilling = "Annen stilling"),
  helsetilstandHindrerArbeid = false,
  andreForholdHindrerArbeid = false
```

## Nuskoder

For alle nuskoder under "3" blir utdanningBestaatt og utdanningGodkjent satt til null.
Se informasjon om nuskoder her: https://www.ssb.no/klass/klassifikasjoner/36

## Styrk08

Standard for yrkesklassifisering: https://www.ssb.no/klass/klassifikasjoner/7

## Azure autentisering

Kall til API-et er autentisert med Bearer token fra Azure.
Eksempel: `Authorization: Bearer <token>`

Machine-to-maching token:
```json
{
  "oid": "989f736f-14db-45dc-b8d1-94d621dbf2bb",
  "roles": ["access_as_application"]
}
```

Logg inn med trygdeetatenbruker på https://azure-token-generator.intern.dev.nav.no/api/m2m?aud=dev-gcp:paw:paw-arbeidssoekerregisteret-api-dolly
for å generere token.

## Teknologier

Øvrige teknologier, rammeverk og biblioteker som er blitt tatt i bruk:

- [**Kotlin**](https://kotlinlang.org/)
- [**Ktor**](https://ktor.io/)
- [**Kafka**](https://kafka.apache.org/)
- [**Gradle**](https://gradle.org/)

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles via issues her på github.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen [#team-paw-dev](https://nav-it.slack.com/archives/CLTFAEW75)

# Lisens

[MIT](LICENSE)
