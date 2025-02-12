# paw-arbeidssoeker-synk-jobb

Jobb for å synke arbeidssøkere i arbeidssøkerregisteret.

## CSV Fil
Jobben forventer å finne en CSV-fil under stien som er definert av `mountPath` variabelen i `job_config.toml`
konfig-filen.

> [!WARNING]  
> Siden CSV-filen inneholder persondata så skal filen opprettes som en secret i Kubernetes

### Opprette CSV-fil som secret i Kubernetes
Informasjon om secrets fra [NAIS docs](https://docs.nais.io/services/secrets/).

```shell
kubectl create secret generic paw-arbeidssoekere-csv --from-file=v1.csv=/tmp/paw-arbeidssoekere-csv/v1.csv
```
