# scripts/

## generate-kafka-signing-keys.sh

Genererer et EC P-256-nøkkelpar for signering av Kafka-meldinger.

- **Privat nøkkel** → lastes opp til NAIS secret (`PAW_SIGNING_PRIVATE_KEY_PKCS8_BASE64` + `PAW_SIGNING_KEY_ID`)
- **Offentlig nøkkel** → commites til repo under `lib/kafka-signing/src/main/resources/paw-signing-public-keys/`

Nøklene genereres i en temporær mappe og makuleres automatisk når scriptet avsluttes.

### Navnekonvensjon

| Parameter | Formel | Eksempel |
|---|---|---|
| Secret name | `{env}-paw-{app}` | `dev-paw-bekreftelse-tjeneste` |
| Key ID | `{env}-paw-{app}-ecdsa-v{version}` | `dev-paw-bekreftelse-tjeneste-ecdsa-v1` |

### Bruk

```bash
./scripts/generate-kafka-signing-keys.sh ENV APP VERSION [OPTIONS]
```

| Argument | Beskrivelse | Eksempel |
|---|---|---|
| `ENV` | Miljø | `dev` eller `prod` |
| `APP` | Kortnavnet på appen | `bekreftelse-tjeneste` |
| `VERSION` | Nøkkelversjon | `1` |

### Eksempler

```bash
# Ny nøkkel for bekreftelse-tjeneste i dev
./scripts/generate-kafka-signing-keys.sh dev bekreftelse-tjeneste 1

# Nøkkelrotasjon for event-processor i prod (versjon 2)
./scripts/generate-kafka-signing-keys.sh prod event-processor 2

# Med navngitte flagg
./scripts/generate-kafka-signing-keys.sh -e dev -a api-bekreftelse -v 1

# Override secret-navn (f.eks. for eldre nøkler med avvikende navn)
./scripts/generate-kafka-signing-keys.sh dev api-inngang 1 --name paw-api-inngang-kafka-signing-key
```

### Alle tilgjengelige flagg

| Flagg | Beskrivelse | Standard |
|---|---|---|
| `-e`, `--env` | Miljø (`dev`\|`prod`) | — |
| `-a`, `--app` | App-kortnavn | — |
| `-v`, `--version` | Nøkkelversjon | — |
| `-n`, `--name` | Override NAIS secret-navn | `{env}-paw-{app}` |
| `-k`, `--key-id` | Override key ID | `{env}-paw-{app}-ecdsa-v{version}` |
| `-p`, `--pub-dir` | Sti til public keys-mappen | `lib/kafka-signing/.../paw-signing-public-keys` |
| `-t`, `--tmp-dir` | tmpfs-basemappe | `/tmp` |
| `-h`, `--help` | Vis hjelp | — |

### Etter at nøkkelen er generert

1. Last opp secret til NAIS (scriptet veileder deg gjennom dette).
2. Legg til key-id i indeksfilen og commit:
   ```bash
   echo 'dev-paw-bekreftelse-tjeneste-ecdsa-v1' >> lib/kafka-signing/src/main/resources/paw-signing-public-keys/index
   git add lib/kafka-signing/src/main/resources/paw-signing-public-keys/
   git commit -m 'feat(kafka-signing): legg til dev-paw-bekreftelse-tjeneste-ecdsa-v1.pub.b64'
   ```
3. Rull ut appen så den plukker opp den nye nøkkelen.
4. **Ved nøkkelrotasjon:** behold gammel `.pub.b64` — den trengs for å verifisere meldinger signert med gammel nøkkel.
