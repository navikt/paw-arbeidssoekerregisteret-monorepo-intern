# kafka-signing

ECDSA P-256-signering og -validering av Kafka-meldinger via producer/consumer interceptors.

## Hvordan det fungerer

`SigningProducerInterceptor` signerer hver utgående melding og legger til to headere:
- `x-paw-signature` — base64url-kodet DER-signatur
- `x-paw-signing-key-id` — ID på nøkkelen som ble brukt

Signaturen dekker: `key || traceparent || timestamp || value`.

`SignatureValidatingConsumerInterceptor` verifiserer signaturen ved å slå opp den offentlige nøkkelen (basert på `x-paw-signing-key-id`) fra classpath-ressursen `paw-signing-public-keys/`.

Manglende, ukjente eller ugyldige signaturer gir en `[kafka-signing]`-advarsel i team-logs, men stopper **ikke** prosessering.

## Oppsett i Kafka Streams

```kotlin
val signingConfig = KafkaSigningConfig(
    mountPath = "/var/run/secrets/kafka-signing",   // Nais secret mount
    localResource = "/local/kafka-signing-key.properties",  // lokal dev
)

val streamsConfig = baseProperties +
    signingConfig.toKafkaStreamsProducerProperties() +
    kafkaStreamsConsumerValidationProperties()

KafkaStreams(topology, StreamsConfig(streamsConfig))
```

**Nais-manifest:**
```yaml
filesFrom:
  - secret: <env>-paw-<app-navn>
    mountPath: /var/run/secrets/kafka-signing
accessPolicy:
  outbound:
    rules:
      - application: logging
        namespace: nais-system
```

**Lokal dev** (`src/main/resources/local/kafka-signing-key.properties`):
```properties
PAW_SIGNING_PRIVATE_KEY_PKCS8_BASE64=<base64-kodet PKCS8 privat nøkkel>
PAW_SIGNING_KEY_ID=dev-paw-<app-navn>-ecdsa-v1
```

## Offentlige nøkler

Offentlige nøkler ligger i `src/main/resources/paw-signing-public-keys/` og er listet i `index`-filen. Alle nøkler lastes ved oppstart og brukes til validering.

Eksempel på `index`:
```
dev-paw-bekreftelse-tjeneste-ecdsa-v1
prod-paw-bekreftelse-tjeneste-ecdsa-v1
```

## Generere nøkkelpar

Bruk scriptet fra repo-roten. Det genererer nøkkelpar i tmpfs, laster opp privat nøkkel til Nais secret via `nais` CLI (eller viser instruksjoner for manuell opplasting), og legger den offentlige nøkkelen i riktig mappe:

```bash
./scripts/generate-kafka-signing-keys.sh \
  --env dev \
  --name dev-paw-<app-navn> \
  --key-id dev-paw-<app-navn>-ecdsa-v1
```

Etter kjøring:
```bash
# Commit offentlig nøkkel og oppdater index
echo 'dev-paw-<app-navn>-ecdsa-v1' >> lib/kafka-signing/src/main/resources/paw-signing-public-keys/index
git add lib/kafka-signing/src/main/resources/paw-signing-public-keys/
git commit -m "feat(kafka-signing): legg til dev-paw-<app-navn>-ecdsa-v1"
```

Den private nøkkelen slettes automatisk av scriptet etter bruk.

## Manuell nøkkelgenerering (uten script)

```bash
openssl ecparam -name prime256v1 -genkey -noout -out ec-private.pem

# Privat nøkkel → NAIS secret (PAW_SIGNING_PRIVATE_KEY_PKCS8_BASE64)
openssl pkcs8 -topk8 -nocrypt -in ec-private.pem -outform DER | base64 -w0

# Offentlig nøkkel → commit til repo
openssl ec -in ec-private.pem -pubout -outform DER | base64 -w0 \
  > lib/kafka-signing/src/main/resources/paw-signing-public-keys/<key-id>.pub.b64

shred -u ec-private.pem
```

## Verifisere en signatur manuelt

```bash
# Hent headere fra en Kafka-melding (key-id og signatur)
KEY_ID="prod-paw-bekreftelse-tjeneste-ecdsa-v1"
PUB_KEY="lib/kafka-signing/src/main/resources/paw-signing-public-keys/${KEY_ID}.pub.b64"

# Dekod offentlig nøkkel
base64 -d "$PUB_KEY" > /tmp/pub.der

# Verifiser signaturen (signerte bytes: key || traceparent || timestamp_be || value)
openssl dgst -sha256 -verify /tmp/pub.der -signature <(base64 -d <<< "$SIGNATURE") <(echo -n "$SIGNED_BYTES")
```

Enklere: se etter `[kafka-signing]`-meldinger i team-logs — valideringsresultatet logges der for alle innkommende meldinger med ukjent nøkkel eller ugyldig signatur.

