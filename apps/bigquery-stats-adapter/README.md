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

## Spørringer

SQL som kjøres manuelt i BigQuery-konsollen, ligger i [`queries/`](queries/). Materialiserte views som applikasjonen oppretter ved oppstart, ligger i `src/main/resources/materialized_views/`.

## Bruk i Grafana

Viewene i `arbeidssoekerregisteret_grafana` kan brukes med BigQuery-datakilden i
Grafana. Velg **Time series** som spørringsformat og **Code** for å skrive SQL.

View som grupperer per dag, eksponerer `dag` som BigQuery `DATE`. Konverter
feltet til `TIMESTAMP` i Grafana-queryen:

```sql
TIMESTAMP(dag, 'Europe/Oslo') AS time
```

Bruk samme uttrykk med Grafanas tidsfilter, og utelat inneværende dag når
statistikken bare skal vise ferdige døgn:

```sql
WHERE $__timeFilter(TIMESTAMP(dag, 'Europe/Oslo'))
  AND dag < CURRENT_DATE('Europe/Oslo')
ORDER BY dag
```

`$__timeFilter` følger tidsintervallet som er valgt i dashboardet. Sett
dashboardets tidssone til `Europe/Oslo` for at døgnene skal vises fra midnatt
norsk tid.

Alle tallkolonner blir egne serier. Tekstkolonner, som `loesning` i viewet for
leverte bekreftelser, blir seriedimensjoner. Grafana kan dermed vise én serie per
kombinasjon av tallkolonne og løsning. Bruk et filter på `loesning` i SQL-en
eller en dashboardvariabel hvis løsningene heller skal vises i separate paneler.

### Periodestarter sammenlignet med tidligere uker

Viewet `arbeidssoekerregisteret_grafana.periodestarter_sammenlignet_med_tidligere_uker`
viser antall periodestarter per dag og antallet på samme ukedag én til fire uker
tidligere. Hver periode telles én gang, også når BigQuery-tabellen inneholder flere
versjoner av perioden.

Viewet bruker en statisk datoserie fordi materialiserte BigQuery-views ikke støtter
`CURRENT_DATE()`. Grafana-spørringen må derfor utelate dagens ufullstendige døgn og
framtidige datoer:

```sql
SELECT
    TIMESTAMP(dag, 'Europe/Oslo') AS time,
    antall_periodestarter,
    antall_1_uke_siden,
    antall_2_uker_siden,
    antall_3_uker_siden,
    antall_4_uker_siden
FROM `arbeidssoekerregisteret_grafana.periodestarter_sammenlignet_med_tidligere_uker`
WHERE $__timeFilter(TIMESTAMP(dag, 'Europe/Oslo'))
  AND dag < CURRENT_DATE('Europe/Oslo')
ORDER BY dag
```

Viewet eksponerer `dag` som `DATE`, lik de andre viewene. Grafana-spørringen
konverterer datoen til et `TIMESTAMP` satt til midnatt i `Europe/Oslo`, slik at
Grafana kan bruke resultatet som tidsakse og med `$__timeFilter`. Tallene viser
vellykkede periodestarter. Et negativt avvik er en indikator på mulig bortfall
under en driftshendelse, ikke et eksakt antall brukere som ble hindret.

### Periodeavslutninger sammenlignet med tidligere uker

Viewet `arbeidssoekerregisteret_grafana.periodeavslutninger_sammenlignet_med_tidligere_uker`
har samme struktur som viewet for periodestarter, men teller perioder gruppert på
`avsluttet.tidspunkt`. Hver periode telles én gang.

```sql
SELECT
    TIMESTAMP(dag, 'Europe/Oslo') AS time,
    antall_periodeavslutninger,
    antall_1_uke_siden,
    antall_2_uker_siden,
    antall_3_uker_siden,
    antall_4_uker_siden
FROM `arbeidssoekerregisteret_grafana.periodeavslutninger_sammenlignet_med_tidligere_uker`
WHERE $__timeFilter(TIMESTAMP(dag, 'Europe/Oslo'))
  AND dag < CURRENT_DATE('Europe/Oslo')
ORDER BY dag
```

### Leverte bekreftelser sammenlignet med tidligere uker

Viewet `arbeidssoekerregisteret_grafana.leverte_bekreftelser_sammenlignet_med_tidligere_uker`
teller leverte bekreftelser per `tidspunkt` og `loesning`. Det følger samme
ukestruktur som periode-viewene, med løsning som en ekstra dimensjon.

```sql
SELECT
    TIMESTAMP(dag, 'Europe/Oslo') AS time,
    loesning,
    antall_leverte_bekreftelser,
    antall_1_uke_siden,
    antall_2_uker_siden,
    antall_3_uker_siden,
    antall_4_uker_siden
FROM `arbeidssoekerregisteret_grafana.leverte_bekreftelser_sammenlignet_med_tidligere_uker`
WHERE $__timeFilter(TIMESTAMP(dag, 'Europe/Oslo'))
  AND dag < CURRENT_DATE('Europe/Oslo')
ORDER BY dag
```