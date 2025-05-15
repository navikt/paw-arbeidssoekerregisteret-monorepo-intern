# paw-api-oppslag-client

Henter informasjon om arbeidssøkere fra API Oppslag.

### Bruk

**_gradle.build.kts_**

```kts
dependencies {
    implementation(project(":lib:http-client-utils"))
    implementation(project(":lib:api-oppslag-client"))
}
```

```kt
import kotlinx.coroutines.runBlocking
import no.nav.paw.tokenprovider.OAuth2TokenProvider
import no.nav.paw.aareg.AaregClient

fun main() {
    val url = "https://modapp-q1.adeo.no/aareg-services"
    val tokenProvider = OAuth2TokenProvider(
        // Token config
    )

    val aaregClient = AaregClient(url) { tokenProvider.getToken() }

    val arbeidsforhold = runBlocking { aaregClient.hentArbeidsforhold("fnr", "callId") }
    println(arbeidsforhold)
}
```
