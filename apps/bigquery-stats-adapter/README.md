# BigQuery Adapter
Dette adapter skriver data fra topic til bigquery.
Identitifikatorer saltes og hashes (sha256) før de sendes. Følgende salts må opprettes ved deploy til en nytt miljø:
```bash
# Salt for arbeidssøkerId
head -c 32 /dev/urandom | kubectl create secret generic bq-enc-hendelse --from-file=enc_hendelse=/dev/stdin
# Salt for periode Id og hendelse Id. De bruker samme salt slik at vi kan koble 'startet' hendelse mot periode og på 
# den måten koble periode mot sha256 verdi for arbeidssøkerId
head -c 32 /dev/urandom | kubectl create secret generic bq-enc-periode --from-file=enc_periode=/dev/stdin
```