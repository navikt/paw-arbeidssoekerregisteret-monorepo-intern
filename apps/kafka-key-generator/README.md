# paw-kafka-key-generator

Et enkelt API for å generere en unik id som skal brukes som "key" ved skriving til topics knyttet til arbeidssøkerregisteret. Tjenesten bruker PDL for å sikre at samme person får samme "key" uavhengig av hvilken "ident" som brukes. Dette er for å sikre at personer som feks bytter fra d-nummer til fødselsnummer får samme "key" og dermed havner i samme partisjon i Kafka.

## Dokumentasjon for API

https://paw-kafka-key-generator.intern.dev.nav.no/docs

## Teknologier

Øvrige teknologier, rammeverk og biblioteker som er blitt tatt i bruk:

- [**Kotlin**](https://kotlinlang.org/)
- [**Ktor**](https://ktor.io/)
- [**PostgreSQL**](https://www.postgresql.org/)
- [**Flyway**](https://flywaydb.org/)
- [**Gradle**](https://gradle.org/)

## Dev oppsett

Eksempel:

```sh
$ curl -XPOST https://paw-kafka-key-generator.intern.dev.nav.no/api/v1/hentEllerOpprett -H 'Authorization: Bearer <access_token>' -d '{"ident": "2072234860133"}'
```

## Lokalt oppsett
Authentisering fungerer ikke lokalt, så det er ikke mulig å teste lokalt på nåværende tidspunkt.
Lokalt kjører løsning mot statisk PDL data som innehlder 2 personer. Data ligger under src/test i no.nav.paw.kafkakeygenerator.testdata.kt.

Under er det satt opp et par ting som må på plass for at applikasjonen og databasen skal fungere. 


### JDK 21

JDK 21 må være installert. Enkleste måten å installere riktig versjon av Java er ved å
bruke [sdkman](https://sdkman.io/install).

### Docker

`docker` og `docker-compose` må være installert.

Start PostgreSQL database
```sh
docker compose -f ../../docker/postgres/docker-compose.yaml up -d
```

Start Kafka broker
```sh
docker compose -f ../../docker/kafka/docker-compose.yaml up -d
```

Start mocks
```sh
docker compose -f ../../docker/mocks/docker-compose.yaml up -d
```

### App

Start app med `./gradlew runTestApp` eller kjør main metoden i 'src/test/kotlin/no/nav/paw/kafkakeygenerator/run_test_app.kt' via Intellij.

### Autentisering

For å kalle APIet lokalt må man være autentisert med et Bearer token.

Vi benytter mock-ouath2-server til å utstede tokens på lokal maskin. Følgende steg kan benyttes til å generere opp et token:

1. Sørg for at containeren for mock-oauth2-server kjører lokalt (docker-compose up -d)
2. Naviger til [mock-oauth2-server sin side for debugging av tokens](http://localhost:8081/default/debugger)
3. Generer et token
4. Trykk på knappen Get a token
5. Skriv inn noe random i "Enter any user/subject" og pid i optional claims, f.eks.

```json
{ "acr": "Level4", "pid": "18908396568" }
```

6. Trykk Sign in
7. Kopier verdien for access_token og benytt denne som Bearer i Authorization-header

8. Eksempel:

```sh
$ curl -XPOST http://localhost:8080/api/v1/hentEllerOpprett -H 'Authorization: Bearer access_token' -d '{"ident": "2072234860133"}'
```

eller benytt en REST-klient (f.eks. [insomnia](https://insomnia.rest/) eller [Postman](https://www.postman.com/product/rest-client/))


## Deploye kun til dev

Ved å prefikse branch-navn med `dev/`, så vil branchen kun deployes i dev.

```
git checkout -b dev/<navn på branch>
```

evt. rename branch

```
git checkout <opprinnlig-branch>
git branch -m dev/<opprinnlig-branch>
```

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles via issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen [#team-paw-dev](https://nav-it.slack.com/archives/CLTFAEW75)

# Lisens

[MIT](LICENSE)
