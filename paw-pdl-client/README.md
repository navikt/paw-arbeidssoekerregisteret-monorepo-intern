# paw-pdl-client

Klient for å gjøre spørringer mot Persondataløsningen [PDL](https://pdldocs-navno.msappproxy.net/ekstern/index.html).

### Bruk av paw-pdl-client

**_gradle.build.kts_**

```kts
dependencies {
    implementation("no.nav.paw.pdl-client:${Versions.pdlClient}")
}
```

### Klienten instansieres slik

For mer informasjon om tema-koder [sjekk her](https://confluence.adeo.no/pages/viewpage.action?pageId=309311397).

```kt
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import no.nav.paw.tokenprovier.OAuth2TokenProvider
import no.nav.paw.pdl-client.PdlCLient

fun main() {
    val url = "http://pdl-api.default.svc.nais.local/graphql"
    val accessTokenProvider = RestSTSAccessTokenProvider()

    val pdlClient = pdlClient(url, "tema", accessTokenProvider::getToken)

    val result = runBlocking { pdlClient.hentAktorId("fnr") }
    println(result)
}
```

### Lokal utvikling

For å teste klienten-endringer i en annen applikasjon uten å publisere remote, kjør:

```sh
./gradlew publishToMavenLocal
```

Pakken blir da publisert til lokalt repository, husk at du må legge til `mavenLocal()` i applikasjonen:

```dsl
repositories {
    mavenLocal()
    mavenCentral()
}
```

### Henvendelser

Spørsmål knyttet til koden eller prosjektet kan rettes mot:

- Jonas Enge <jonas.maccyber.enge@nav.no>

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team-paw-dev.
