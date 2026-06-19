# Generating a new key pair

```bash
openssl ecparam -name prime256v1 -genkey -noout -out ec-private.pem

# Private key → goes into NAIS secret (PAW_SIGNING_PRIVATE_KEY_PKCS8_BASE64)
openssl pkcs8 -topk8 -nocrypt -in ec-private.pem -outform DER | base64 -w0

# Public key → commit here
openssl ec -in ec-private.pem -pubout -outform DER | base64 -w0 > paw-api-inngang-ecdsa-v1.<env>.pub.b64

# Shred the private key file when done
shred -u ec-private.pem

