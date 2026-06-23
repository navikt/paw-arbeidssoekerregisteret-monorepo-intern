# Designskisse: Kryptografisk Audit API for Arbeidssøkerregisteret

Dette dokumentet beskriver en planlagt løsning for å skape en passiv, verifiserbar audit-trail av hendelser i arbeidssøkerregisteret ved hjelp av Kafka meldings-signering. 

Målet er å kunne bygge et audit API som aggregerer data på tvers av topics, bygger en kausal graf over hendelser, og utfører kryptografisk verifisering for å avsløre spoofing, uautoriserte endringer, eller "out of band" Kafka-meldinger.

## Datagrunnlag: Rådata i PostgreSQL

For å kunne garantere beviskjeden må vi lagre rå, uendrede bytes fra Kafka i en PostgreSQL-database. Et eksternt verktøy (f.eks. en Rust raw-data collector) konsumerer topics og lagrer dataene uten å berøre bytes som inngår i signaturgunnlaget.

**Anbefalt tabellstruktur for rådata (`kafka_raw_events`):**
- `topic` (String)
- `partition` (Int)
- `offset` (BigInt)
- `timestamp` (BigInt) - Meldingens timestamp (inngår i signatur)
- `traceparent` (String, nullable) - Trukket ut fra headere, inngår i signatur
- `trace_id` (String, nullable) - Parsed fra traceparent for rask indeksering
- `signatur_id` (String) - Hentet fra `paw.signature.id` header
- `signatur` (ByteA) - Hentet fra `paw.signature` header
- `key_bytes` (ByteA) - Kafka-nøkkel
- `value_bytes` (ByteA) - Avro payload

*Unik skranke:* `(topic, partition, offset)` brukes for deduplikering, da Kafka har at-least-once levering.

---

## Arkitektur: De tre lagene i Audit-motoren

### Lag 1: Kryptografisk verifisering
Det første API-et gjør er å re-verifisere den kryptografiske signaturen (ECDSA) for alle innleste records.
- Henter offentlig nøkkel basert på `signatur_id` (støtter historiske nøkler tross rotasjon, så lenge de beholdes i databasen/nøkkelregisteret).
- Kalkulerer signatur-grunnlaget: lengde-prefiksede bytes av nøkkel, traceparent, timestamp og value.
- Klassifiserer hver rad som: `GYLDIG`, `UGYLDIG_SIGNATUR`, `UKJENT_NØKKEL`, eller `USIGNERT`.

### Lag 2: Kausal graf (Causal Graph)
Hendelser knyttes sammen ved hjelp av `trace_id` (ekstrahert fra `traceparent`).
- Meldinger sorteres topologisk etter `timestamp`.
- Grafen viser hendelsesforløpet (eks: `bekreftelse-api` mottar request -> genererer Kafka melding -> `bekreftelse-hendelsefilter` validerer og videresender -> `bekreftelse-tjeneste` oppdaterer tilstand).
- Systemet må håndtere records uten traceparent på en grasiøs måte (legitime "isolated nodes"). *Merk: Dette krever at OpenTelemetry context propagation (OTel) fungerer konsistent på tvers av Kafka Streams-applikasjoner.*

### Lag 3: Avro Deserialisering og Innholdsvalidering
Siden databasen kun lagrer `value_bytes`, deserialiseres innholdet "lazy" ved oppslag, enten ved bruk av et Schema Registry eller forhåndskompilerte Avro-skjemaer.

Her oppstår selve verdien i API-et: **Kryssvalidering av innhold mot signatur.**
*Eksempel:* En melding på `paw.arbeidssoker-bekreftelse-v1` har `bekreftelsesloesning = ARBEIDSSOEKERREGISTERET`. Audit-motoren har en regel som sier at dette *kun* kan signeres av `bekreftelse-api`. Dersom meldingen har en gyldig signatur fra en annen app, eller mangler signatur, flagges den som en sikkerhetsanomali (spoofing-forsøk).

---

## Foreslåtte Audit API Endpoints

- `GET /audit/arbeidssoeker/{periodeId}`
  Henter alle hendelser for en gitt periode/arbeidssøker, deserialisert, med verifisert signaturstatus på hver hendelse.
- `GET /audit/trace/{traceId}`
  Visualiserer eller lister den fulle kjeden (causal graph) av en spesifikk transaksjon.
- `GET /audit/anomalier`
  Returnerer en liste over hendelser som feiler kryssvalidering, har ugyldig signatur, mangler traceparent uventet, etc.
- `GET /audit/topics/oppsummering`
  Statistikk på signatur-helse (antall gyldige, ukjente nøkler, etc.) per topic i et tidsrom.

---

## Svakheter og Forutsetninger som må håndteres

1. **Nøkkelrotasjon og lagring:** Audit-løsningen må beholde alle historiske offentlige nøkler knyttet til sin `signatur_id` til evig tid (eller i det minste i hele revisjonsperioden), ellers vil historiske gyldige hendelser feile som `UKJENT_NØKKEL`.
2. **Database Integritet:** Rå-bytes i PostgreSQL er "Source of Truth". Så lenge disse ikke er tuklet med, kan signaturen alltid re-verifiseres. Selve databasen bør ha strenge tilgangskontroller, men siden den kan re-verifisere seg selv, vil tukling med innhold oppdages (signaturen brekker). Tukling med å *slette* records er vanskeligere å oppdage uten kontinuerlig offset-tracking.
3. **Avhengighet til Schema Registry:** Audit API-et trenger historiske schemaversjoner for å lese gamle records.
4. **Sporbarhet (Traceparent):** Systemet lener seg tungt på OTel. Hvis en app glemmer å propagere traceparent, brytes lenken i den kausale grafen.
5. **Forventet Signatur-mapping:** Det må vedlikeholdes en konfigurasjon i Audit-systemet som mapper Topic + Innhold -> Forventet `signatur_id` prefix.

## Veien videre: Passiv changelog-validering
Neste steg for å styrke sikkerheten rundt Kafka Streams' interne tilstander er å aktivere `restoreConsumerPrefix` i Kafka-signing-biblioteket. Selv om vi kanskje aldri hindrer en streams-app i å boote på tuklet state, vil en Audit-løsning kunne lese gjennom alle changelog-topics passivt og garantere at hver eneste entry stammer fra en autorisert kilde. Eneste blokkering her er bootstrapping av eksisterende miljøer (historiske records uten signatur). Dette kan løses når rammeverket støtter en cut-off timestamp for streng validering.