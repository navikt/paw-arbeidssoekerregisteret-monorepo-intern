# Kafka-signering i PAW arbeidssøkerregisteret

Biblioteket `lib/kafka-signing` gir ECDSA P-256-signering og -validering av Kafka-meldinger via producer/consumer interceptors. Signaturen dekker `key || traceparent || timestamp || value` og legges som headers (`x-paw-signature`, `x-paw-signing-key-id`) på hver melding.

Validering gir `[KAFKA_SIGNING]`-merkede advarsler i teamets logger ved ukjente nøkler eller ugyldige signaturer, men stopper ikke prosessering. Google Cloud Monitoring fanger opp ERROR og WARNING tagget `KAFKA_SIGNING` og sender alarm til Slack-kanalen **#paw-kafka-signing-alerts**.

---

## Del 1 – Hva er gjort

### Oversikt per applikasjon

| Applikasjon | Rolle | Topics involvert |
|---|---|---|
| `api-start-stopp-perioder` | Signerer utgående meldinger | `paw.arbeidssoker-hendelseslogg-v1` |
| `bekreftelse-api` | Signerer utgående meldinger | `paw.arbeidssoker-bekreftelse-v1` |
| `bekreftelse-tjeneste` | Signerer + validerer (Kafka Streams) | Inn: `paw.arbeidssokerperioder-v1`, `paw.arbeidssoker-bekreftelse-v1` → Ut: `paw.arbeidssoker-bekreftelse-hendelseslogg-v1` |
| `bekreftelse-hendelsefilter` | Signerer utgående meldinger (Kafka Streams) | Filtrerte bekreftelse- og paaVegneAv-topics |
| `bekreftelse-utgang` | Signerer + validerer (Kafka Streams) | Inn: `paw.arbeidssokerperioder-v1`, `paw.arbeidssoker-bekreftelse-hendelseslogg-v1` → Ut: `paw.arbeidssoker-hendelseslogg-v1` |
| `hendelseprosessor` | Validerer innkommende + fjerner signeringsheadere | Inn: `paw.arbeidssoker-hendelseslogg-v1` → Ut: `periode`, `opplysninger` (usignert) |
| `dolly-api` | Legger på statisk ugyldig signatur (dev-testing) | `paw.arbeidssoker-hendelseslogg-v1` |

### Detaljer per applikasjon

#### `api-start-stopp-perioder`
Bruker `KafkaConfig.withRecordSigning(signingConfig)` som aktiverer `SigningProducerInterceptor` på Kafka-produsenten. Alle hendelser som skrives til hendelsesloggen signeres.

Registrerte nøkler:
- `prod-paw-api-inngang-ecdsa-v1`

#### `bekreftelse-api`
Bruker `KafkaConfig.withRecordSigning(signingConfig)`. Signerer bekreftelser som brukere sender inn.

Registrerte nøkler:
- `prod-paw-api-bekreftelse-ecdsa-v1`

#### `bekreftelse-tjeneste`
Kafka Streams-applikasjon som bruker `signingConfig.toKafkaStreamsProducerProperties()` for signering og `kafkaStreamsConsumerValidationProperties()` for validering. Validerer innkommende meldinger fra periode- og bekreftelse-topics.

Registrerte nøkler:
- `prod-paw-bekreftelse-tjeneste-ecdsa-v1`

#### `bekreftelse-hendelsefilter`
Kafka Streams-applikasjon som kun bruker `signingConfig.toKafkaStreamsProducerProperties()`. Filtrerer bekreftelser fra dagpenger og flex-topics og republiserer til interne topics med PAW-signatur.

Registrerte nøkler:
- `prod-paw-bekreftelse-filter-ecdsa-v1`

#### `bekreftelse-utgang`
Kafka Streams-applikasjon med både signering (`toKafkaStreamsProducerProperties()`) og validering (`kafkaStreamsConsumerValidationProperties()`). Konsumerer perioder og bekreftelseshendelser og produserer til hendelsesloggen.

Registrerte nøkler:
- `prod-paw-bekreftelse-utgang-ecdsa-v1`

#### `hendelseprosessor`
Kafka Streams-applikasjon som kun validerer innkommende meldinger fra hendelsesloggen (`kafkaStreamsConsumerValidationProperties()`). Fjerner i tillegg signeringsheadere (`stripSigningHeaders`) før meldinger skrives til nedstrøms topics, slik at consumers på periode og opplysninger ikke forventer en signatur som ikke er der.

`hendelseprosessor` har ingen registrert signing-nøkkel og signerer ikke.

#### `dolly-api`
Legger manuelt på en statisk, kjent-ugyldig signatur med nøkkel-ID `paw-api-inngang-kafka-signing-key-v2`. Dette er bevisst — formålet er å verifisere at consumers i dev-miljø håndterer ugyldige signaturer korrekt uten å krasje.

### Registrerte offentlige nøkler

Alle offentlige nøkler ligger i `lib/kafka-signing/src/main/resources/paw-signing-public-keys/` og listes i `index`-filen. Nøklene lastes ved oppstart og brukes til validering av alle innkommende meldinger.

```
prod-paw-api-inngang-ecdsa-v1
prod-paw-api-bekreftelse-ecdsa-v1
prod-paw-bekreftelse-tjeneste-ecdsa-v1
prod-paw-bekreftelse-filter-ecdsa-v1
prod-paw-bekreftelse-utgang-ecdsa-v1
```

### Overvåking og alerting

Google Cloud Monitoring er satt opp til å sende alarm til **#paw-kafka-signing-alerts** ved ERROR eller WARNING tagget `KAFKA_SIGNING` i teamets logger. Dette dekker:

- Ukjent nøkkel-ID (melding signert med nøkkel vi ikke kjenner)
- Ugyldig signatur (melding er manipulert eller nøkkel er rotert uten oppdatering)
- Manglende signatur der validering er aktivert

**Unntak:** Periode-topic (`paw.arbeidssokerperioder-v1`) er ikke signert (`hendelseprosessor` mangler signing-nøkkel), og alarm er ikke aktivert for meldinger på dette topicet.

---

## Del 2 – Hva gjenstår

### 1. Kafka Streams state stores

Kafka Streams bruker interne changelog-topics for å persistere state stores (tilstandslagre). Disse topics inneholder applikasjonsintern tilstand, men skrives til Kafka og kan i prinsippet manipuleres av noen med tilgang til clusteret.

Berørte applikasjoner:
- `bekreftelse-tjeneste` — state stores for `BekreftelseTilstand` og `PaaVegneAvTilstand`
- `bekreftelse-utgang` — state store for `InternTilstand`
- `hendelseprosessor` — state store for arbeidssøker-periodetilstand

State stores signeres ikke i dag fordi `SigningProducerInterceptor` ikke trigges for interne changelog-writes i Kafka Streams. For å signere state stores kreves en annen mekanisme, for eksempel tilpasset serde-wrapper eller en dedikert interceptor som kun aktiveres for changelog-topics.

**Vurdering:** State stores er interne og ikke tilgjengelige for andre tjenester uten eksplisitt tilgang. Angrepsvektoren er lavere enn for topic-to-topic-flyt, men bør adresseres for fullstendig integritetssikring.

### 2. Public topics: periode, opplysninger og profilering

`hendelseprosessor` transformerer hendelser fra den interne hendelsesloggen til tre public topics som konsumeres av tjenester på tvers av PAW og av ekstern-monorepo:

| Topic | Status |
|---|---|
| `paw.arbeidssokerperioder-v1` | ❌ Ikke signert — alarm deaktivert (se Del 1) |
| `paw.opplysninger-om-arbeidssoeker-v1` | ❌ Ikke signert — ikke validert |
| Profilering (eksternt) | ❌ Ikke signert — ikke validert |

For å signere disse topics må:
1. `hendelseprosessor` få en signing-nøkkel (Nais secret + registrert public key i `lib/kafka-signing`)
2. `signingConfig.toKafkaStreamsProducerProperties()` legges til i Streams-konfigurasjonen
3. Consumers som leser disse topics og ønsker validering, aktiverer `kafkaStreamsConsumerValidationProperties()`
4. GCM-alarmen for periode-topic aktiveres

---

## Del 3 – Bruk i fremtidig intern kontroll

> **Merk:** Dette er løse ideer og brainstorming — ikke en konkret plan eller noe som er under aktiv utvikling.

Signeringsinfrastrukturen gir et grunnlag for intern kontroll av dataintegritet og meldingsopprinnelse. Her er noen tanker om hvordan dette kan brukes videre.

### Revisjonsspor for meldingsflyt

Fordi signaturen dekker `key || traceparent || timestamp || value`, er det mulig i ettertid å verifisere at:

- En melding ikke er endret etter produksjon
- Meldingen kom fra den tjenesten som påstår å ha sendt den (via nøkkel-ID)
- Tidsstempelet på meldingen stemmer med header-verdien

Et intern-kontroll-verktøy kan periodisk sample meldinger fra kritiske topics og verifisere signaturer programmatisk, uten å involvere produksjonssystemene.

### Deteksjon av uautoriserte produsenter

Alle kjente produsenter har registrerte offentlige nøkler. En melding med ukjent `x-paw-signing-key-id` — eller uten signeringsheadere — indikerer at noen har skrevet til topicet uten å gå gjennom godkjente tjenester.

Et kontrollverktøy kan overvåke:
- Andel usignerte meldinger per topic over tid
- Ukjente nøkkel-IDer
- Signaturfeil per nøkkel (kan indikere rotasjonsproblemer)

Disse metrikene kan eksponeres som Prometheus-gauges og vises i eksisterende Grafana-dashboards for PAW.

### Nøkkelrotasjon og livssyklus

Nøkkel-IDene følger et navnemønster (`<env>-paw-<app>-ecdsa-v1`) som gjør det enkelt å spore hvilken tjeneste og miljø en nøkkel tilhører. Et kontrollverktøy kan:

- Varsle når en nøkkel ikke har blitt brukt på en stund (mulig avvikling uten opprydding)
- Varsle når mange meldinger sendes med legacy-nøkler som burde vært faset ut
- Gi oversikt over nøkkeldekning per topic: hvilke topics har 100 % signerte meldinger, og hvilke har gap

### Integrasjon med eksisterende alerting

GCM-alarmen mot `#paw-kafka-signing-alerts` er et godt utgangspunkt. Fremtidig intern-kontroll kan bygge på dette ved å:

- Aggregere alarmer per topic og tjeneste i en kontroll-rapport
- Koble signeringsstatus til deploylogg — en ny deploy som plutselig produserer usignerte meldinger er et tydelig signal om at noe er feil
- Bruke `traceparent`-headeren (som inngår i signaturen) til å korrelere meldinger på tvers av tjenester i samme request-kjede, og dermed verifisere at en melding faktisk stammer fra en legitimert brukeraksjon

### Årsak-virkning-sporing ved mistenkelige meldinger

Fordi `traceparent` inngår i signaturen, er trace-ID kryptografisk bundet til meldingsinnholdet — den kan ikke endres uten at signaturen bryter. Dette gjør trace-ID til et sterkt sporingsanker ved hendelsesanalyse.

Merk at Tempo og Loki har kort lagringstid, så ved etterforskning som skjer noe tid etter hendelsen vil vi i stor grad måtte basere oss på det vi selv lagrer. Det betyr at trace-ID primært er nyttig som korrelasjonsnøkkel mot våre egne Kafka-topics og eventuelle egne audit-logger — ikke som en vei inn i observabilitetsplattformen.

Dersom en melding med ugyldig eller manglende signatur oppdages, kan trace-ID brukes til å rekonstruere årsak-virkningskjeden:

1. **Finn opphavet i hendelsesloggen** — trace-ID er lagret som header på meldingen i `paw.arbeidssoker-hendelseslogg-v1`. Ved å søke på trace-ID her kan man finne den opprinnelige hendelsen, tidspunktet og nøkkel-ID som ble brukt.
2. **Følg flyten fremover** — samme trace-ID propageres til nedstrøms topics (periode, opplysninger). Det er dermed mulig å se hvilke records som ble produsert som følge av den mistenkelige meldingen.
3. **Avgrens skadeomfang** — ved å søke på trace-ID på tvers av alle PAW-topics er det mulig å identifisere nøyaktig hvilke records som er berørt, uten å måtte gå gjennom hele hendelsesloggen.

Et intern-kontroll-verktøy kan automatisere dette: søk opp trace-ID direkte i Kafka ved signaturfeil og generer en hendelsesrapport med involvert tjeneste, nøkkel og meldingsflyt.

### Idé: Passiv audit-motor med rådata i PostgreSQL

For å kunne garantere en fullstendig beviskjede, uavhengig av Kafkas egen retention, kan rådata fra Kafka lagres uendret i PostgreSQL. Et eksternt verktøy (f.eks. en enkel Rust-basert consumer) lagrer meldingene uten å tolke dem:

| Felt | Beskrivelse |
|---|---|
| `topic`, `partition`, `offset` | Unik identifikator (brukes for deduplisering ved at-least-once-levering) |
| `timestamp` | Meldingstidsstempel — inngår i signaturen |
| `traceparent` | Trukket ut fra headere — inngår i signaturen |
| `trace_id` | Parsert fra `traceparent` for rask indeksering |
| `signing_key_id` | Fra `x-paw-signing-key-id`-headeren |
| `signature` | Fra `x-paw-signature`-headeren (rå bytes) |
| `key_bytes` | Kafka-nøkkelen |
| `value_bytes` | Avro-payloaden |

Med dette som grunnlag kan en audit-motor gjøre tre ting:

1. **Kryptografisk verifisering** — re-verifisere ECDSA-signaturen for alle lagrede records og klassifisere dem som `GYLDIG`, `UGYLDIG_SIGNATUR`, `UKJENT_NØKKEL` eller `USIGNERT`.
2. **Kausal graf** — koble hendelser sammen via `trace_id` og sortere dem topologisk etter tidsstempel. Dette viser hendelsesforløpet på tvers av topics og tjenester for en gitt transaksjon.
3. **Kryssvalidering av innhold** — verifisere at meldinger på et gitt topic er signert av riktig tjeneste. En bekreftelse signert av noe annet enn `bekreftelse-api` er et tydelig anomalisignal.

**Forutsetninger og svakheter:**
- Alle historiske offentlige nøkler må bevares — ellers vil gamle gyldige meldinger feile som `UKJENT_NØKKEL`
- Sletting av records i databasen kan ikke oppdages av signeringsmekanismen alene; kontinuerlig offset-tracking er nødvendig
- Audit-motoren trenger tilgang til historiske Avro-skjemaversjoner fra Schema Registry for å deserialisere gamle records
- Dersom en app ikke propagerer `traceparent`, brytes lenken i den kausale grafen

