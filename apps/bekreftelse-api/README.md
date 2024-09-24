# Bekreftelse API

```mermaid
graph TD
    A[POST /rapportering] --> B(Rapportering-api instans)
    B --> |POST identitetsnummer| C[Kafka-key-generator]
    C -->|arbeidssøkerId| B
    B --> D["streams.metadataForKey('arbeidssøkerId', 'RapporteringStateStore')"]
    D --> E{Finne hvilke instans som har data for arbeidssøkerId}
    E -->|Funnet remote: forward HTTP request| Z["Rapportering-api instans (med arbeidssøkerId)"]
    Z -->|Returner respons| B
    E -->|Funnet i lokal stateStore spør mot den| F[RapporteringStateStore<br>key = arbeidssøkerId<br/>value = List<RapporteringTilgjengelig>]
    G[RapporteringTilgjengelig] -->|Legg til rapportering i liste| F
    H[RapporteringsMeldingMottatt]-->|slett rapportId fra liste|F
    I[PeriodeAvsluttet] --> |slett arbeidssøkerId|F
```

## Autentisering
Applikasjonen er beskyttet som en **OAuth2 Resource Server** vha **NAV Security Token Validator** fellesbibliotek.

Andre applikasjoner som skal kalle APIet må benytte **OAuth2 Access Token** som **Bearer Token** i kall.

### IDPorten
Ved innlogging av sluttbruker på **Min Side** benyttes **Access Token** fra **IdPorten**.

**NAIS** tilbyr en **Login-Proxy** som benytter session cookie til å slå opp og legge ved Bearer Token i kallet
mot APIet.

NAIS-dokumentasjon:
* [Login-Proxy](https://doc.nais.io/auth/explanations/#login-proxy)
* [IdPorten Auth How-To](https://doc.nais.io/auth/idporten/how-to/login/)
* [IdPorten Auth Reference](https://doc.nais.io/auth/idporten/reference/)

### Entra ID (Azure AD)
### TokenX
