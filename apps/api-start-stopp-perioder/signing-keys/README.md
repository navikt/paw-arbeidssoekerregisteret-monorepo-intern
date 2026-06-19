# Kafka Signing Keys — paw-arbeidssokerregisteret-api-inngang

Public keys used to verify ECDSA signatures on Kafka messages produced by this app.

Private keys are stored in NAIS secrets (`paw-api-inngang-kafka-signing-key`) and never committed to git.

## Key files

| File | Environment | Key ID |
|------|-------------|--------|
| `paw-api-inngang-ecdsa-v1.dev.pub.b64` | dev-gcp | `paw-api-inngang-ecdsa-v1` |
| `paw-api-inngang-ecdsa-v1.prod.pub.b64` | prod-gcp | `paw-api-inngang-ecdsa-v1` |

Each file contains the Base64-encoded DER bytes of the EC public key (prime256v1 / P-256).

## Signature format

Every Kafka record produced by this app has two headers:

| Header | Content |
|--------|---------|
| `x-paw-signature` | Base64url (no padding), DER-encoded SHA256withECDSA signature |
| `x-paw-signing-key-id` | Key ID — use to look up the correct public key file |

The signature covers the following payload (length-prefixed fields):

```
[4B len][record key bytes]
[4B len][traceparent bytes (UTF-8, empty if absent)]
[8B    ][record timestamp, big-endian int64 milliseconds]
[4B len][record value bytes]
```

## Offline verification (example)

```bash
# Decode the public key from file
base64 -d paw-api-inngang-ecdsa-v1.dev.pub.b64 > ec-public.der

# Decode the signature header value (Base64url → DER)
echo "<x-paw-signature header value>" | tr '_-' '/+' | base64 -d > signature.der

# Build the signed payload manually and write to payload.bin, then verify:
openssl dgst -sha256 -verify ec-public.der -signature signature.der payload.bin
```

For programmatic verification use `verifyKafkaRecord()` from `lib:kafka-signing`:

```kotlin
val publicKey = KeyFactory.getInstance("EC")
    .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(pubKeyB64))) as ECPublicKey

val valid = verifyKafkaRecord(
    keyBytes       = recordKey,
    traceparentBytes = headers["traceparent"] ?: ByteArray(0),
    timestampMs    = recordTimestamp,
    valueBytes     = recordValue,
    signatureBytes = Base64.getUrlDecoder().decode(headers["x-paw-signature"]),
    publicKey      = publicKey
)
```

## Rotating keys

1. Generate a new key pair (see below)
2. Update the NAIS secret `paw-api-inngang-kafka-signing-key` in [console.nais.io](https://console.nais.io)
3. Add the new public key file here with an incremented version suffix (e.g. `ecdsa-v2`)
4. Keep the old public key file — it is still needed to verify messages signed before rotation

## Generating a new key pair

```bash
openssl ecparam -name prime256v1 -genkey -noout -out ec-private.pem

# Private key → goes into NAIS secret (PAW_SIGNING_PRIVATE_KEY_PKCS8_BASE64)
openssl pkcs8 -topk8 -nocrypt -in ec-private.pem -outform DER | base64 -w0

# Public key → commit here
openssl ec -in ec-private.pem -pubout -outform DER | base64 -w0 > paw-api-inngang-ecdsa-v1.<env>.pub.b64

# Shred the private key file when done
shred -u ec-private.pem
```
