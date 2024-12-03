# paw-kafka-key-generator

Et enkelt API for å generere en unik id som skal brukes som "key" ved skriving til topics knyttet til arbeidssøkerregisteret. Tjenesten bruker PDL for å sikre at samme person får samme "key" uavhengig av hvilken "ident" som brukes. Dette er for å sikre at personer som feks bytter fra d-nummer til fødselsnummer får samme "key" og dermed havner i samme partisjon i Kafka.

## Dokumentasjon for API

https://paw-kafka-key-generator.intern.dev.nav.no/docs

## Teknologier

Øvrige teknologier, rammeverk og biblioteker som er blitt tatt i bruk:

- [**Kotlin**](https://kotlinlang.org)
- [**Ktor**](https://ktor.io)
- [**PostgreSQL**](https://www.postgresql.org)
- [**Flyway**](https://flywaydb.org)
- [**Kafka**](https://kafka.apache.org)
- [**Gradle**](https://gradle.org)

## Dev oppsett

### JDK 21

JDK 21 må være installert. Enkleste måten å installere riktig versjon av Java er ved å
bruke [sdkman](https://sdkman.io/install).

### Docker

[Docker](https://docs.docker.com) og [Docker Compose](https://docs.docker.com/compose) må være installert.

#### Start PostgreSQL database
```shell
docker compose -f ../../docker/postgres/docker-compose.yaml up -d
```

#### Start Kafka broker
```shell
docker compose -f ../../docker/kafka/docker-compose.yaml up -d
```

#### Start mocks
Benytter mock [OAuth2 server](https://github.com/navikt/mock-oauth2-server) fra NAV Security og mock PDL vha [Wiremock](https://wiremock.org). 
```shell
docker compose -f ../../docker/mocks/docker-compose.yaml up -d
```

### App

#### Gradle
Start appen vha Gradle sin [application plugin](https://docs.gradle.org/current/userguide/application_plugin.html).
```shell
../../gradlew :apps:kafka-key-generator:run
```

Alternativt test-oppsett.
```shell
../../gradlew :apps:kafka-key-generator:runTestApp
```

#### IntelliJ
Start appen ved å kjøre `main` funksjonen i `./src/main/kotlin/no/nav/paw/kafkakeygenerator/AppStarter.kt`.

Alternativt test-oppsett i `./src/test/kotlin/no/nav/paw/kafkakeygenerator/TestAppStarter.kt`.

### Autentisering
Applikasjonen er sikret som en OAuth2 Resource Server. For å kalle APIet må man sende med et OAuth2 Bearer Token.

For å hente token fra `mock-oauth2-server` gjør følgende request med `curl`:
```shell
ACCESS_TOKEN="$(curl -X POST http://localhost:8081/azure/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=paw-kafka-key-generator&client_secret=abcd1234&scope=openid%20pid" \
| jq .access_token)"
```

### Gjøre kall

```sh
$ curl -X POST http://localhost:8080/api/v1/hentEllerOpprett -H "Authorization: Bearer ${ACCESS_TOKEN}" -d '{"ident": "01017012345"}'
```

Kan også benytte en grafisk REST-klient (f.eks. [insomnia](https://insomnia.rest/) eller [Postman](https://www.postman.com/product/rest-client/))

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
