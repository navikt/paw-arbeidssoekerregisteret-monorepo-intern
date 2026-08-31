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

Ved oppstart oppretter applikasjonen views som mangler. For eksisterende views
synkroniserer den automatisk refresh-intervallet og maksimal datastaleness fra
`MaterializedViewGenerator.kt`. SQL-definisjonen endres ikke; den krever at
viewet opprettes på nytt.

### Oversikt over materialiserte views

Tabellene under beskriver alle viewene applikasjonen oppretter i datasettet
`arbeidssoekerregisteret_grafana`. **Grafana** åpner brukseksemplet, mens
**SQL** åpner definisjonen av viewet.

#### Perioder og varighet

| View | Innhold |
| --- | --- |
| `aktive_by_weeks_since_start` ([Grafana](#aktive_by_weeks_since_start), [SQL](src/main/resources/materialized_views/aktive_by_weeks_since_start.sql)) | Antall aktive perioder per dag, fordelt på hele uker siden periodestart. |
| `aktive_dag_for_dag` ([Grafana](#aktive_dag_for_dag), [SQL](src/main/resources/materialized_views/aktive_dag_for_dag.sql)) | Samlet antall aktive perioder per dag. |
| `aktive_duration_stats_per_month` ([Grafana](#aktive_duration_stats_per_month), [SQL](src/main/resources/materialized_views/aktive_duration_stats_per_month.sql)) | Antall aktive perioder ved starten av hver måned, med median, maksimum og varighetsintervaller i uker. |
| `alle_perioder` ([Grafana](#alle_perioder), [SQL](src/main/resources/materialized_views/alle_perioder.sql)) | Én rad per periode med start- og sluttdata. Avsluttet versjon foretrekkes når en periode har flere rader. |
| `avsluttede_perioder_varighet_distribusjon_per_maaned` ([Grafana](#avsluttede_perioder_varighet_distribusjon_per_maaned), [SQL](src/main/resources/materialized_views/avsluttede_perioder_varighet_distribusjon_per_maaned.sql)) | Antall avsluttede perioder per måned, avslutningsårsak og varighetsintervall. |
| `avsluttede_perioder_varighet_stats_per_maaned` ([Grafana](#avsluttede_perioder_varighet_stats_per_maaned), [SQL](src/main/resources/materialized_views/avsluttede_perioder_varighet_stats_per_maaned.sql)) | Varighetsstatistikk per avslutningsmåned og årsak, med gjennomsnitt, persentiler og antall korte, mellomlange og lange perioder. |
| `avsluttet_by_aarsak` ([Grafana](#avsluttet_by_aarsak), [SQL](src/main/resources/materialized_views/avsluttet_by_aarsak.sql)) | Antall periodeavslutninger per dag og avslutningsårsak. |
| `avsluttet_by_aarsak_brukertype` ([Grafana](#avsluttet_by_aarsak_brukertype), [SQL](src/main/resources/materialized_views/avsluttet_by_aarsak_brukertype.sql)) | Antall periodeavslutninger per dag, avslutningsårsak og brukertype. |
| `korttid_langtid_opplysninger_ved_start` ([Grafana](#korttid_langtid_opplysninger_ved_start), [SQL](src/main/resources/materialized_views/korttid_langtid_opplysninger_ved_start.sql)) | Avsluttede perioder fordelt på opplysninger ved start, brukertype og om perioden varte høyst eller mer enn 182 dager. |
| `periodeavslutninger_sammenlignet_med_tidligere_uker` ([Grafana](#periodeavslutninger_sammenlignet_med_tidligere_uker), [SQL](src/main/resources/materialized_views/periodeavslutninger_sammenlignet_med_tidligere_uker.sql)) | Daglige periodeavslutninger sammenlignet med samme ukedag de fire foregående ukene. |
| `periodestarter_sammenlignet_med_tidligere_uker` ([Grafana](#periodestarter_sammenlignet_med_tidligere_uker), [SQL](src/main/resources/materialized_views/periodestarter_sammenlignet_med_tidligere_uker.sql)) | Daglige periodestarter sammenlignet med samme ukedag de fire foregående ukene. |
| `startet_by_tid_siden_avsluttet_aarsak` ([Grafana](#startet_by_tid_siden_avsluttet_aarsak), [SQL](src/main/resources/materialized_views/startet_by_tid_siden_avsluttet_aarsak.sql)) | Aktive perioder fordelt på brukertype, forrige avslutningsårsak og tid siden forrige periode ble avsluttet. |
| `tilbakevending_etter_avsluttet_per_maaned` ([Grafana](#tilbakevending_etter_avsluttet_per_maaned), [SQL](src/main/resources/materialized_views/tilbakevending_etter_avsluttet_per_maaned.sql)) | Avsluttede perioder per måned og årsak, fordelt på hvor lang tid det tok før neste periode startet. |

#### Bekreftelser

| View | Innhold |
| --- | --- |
| `aktive_perioder_by_har_jobbet_dag_for_dag` ([Grafana](#aktive_perioder_by_har_jobbet_dag_for_dag), [SQL](src/main/resources/materialized_views/aktive_perioder_by_har_jobbet_dag_for_dag.sql)) | Aktive perioder per dag fordelt på `JA`, `NEI` og `UKJENT` ut fra siste bekreftelse som dekker dagen. |
| `avsluttede_perioder_andel_har_jobbet_per_dag` ([Grafana](#avsluttede_perioder_andel_har_jobbet_per_dag), [SQL](src/main/resources/materialized_views/avsluttede_perioder_andel_har_jobbet_per_dag.sql)) | Daglige periodeavslutninger og gjennomsnittlig andel positive jobbsvar i de siste 2, 10 og 20 bekreftelsene. |
| `avsluttede_perioder_sammenhengende_har_jobbet_per_uke` ([Grafana](#avsluttede_perioder_sammenhengende_har_jobbet_per_uke), [SQL](src/main/resources/materialized_views/avsluttede_perioder_sammenhengende_har_jobbet_per_uke.sql)) | Ukentlige periodeavslutninger fordelt på antall sammenhengende positive jobbsvar ved avslutning. |
| `bekreftelse_jobb_og_fortsettelse_per_maaned` ([Grafana](#bekreftelse_jobb_og_fortsettelse_per_maaned), [SQL](src/main/resources/materialized_views/bekreftelse_jobb_og_fortsettelse_per_maaned.sql)) | Månedlige bekreftelser per løsning og brukertype, med antall og andeler for arbeid og ønske om å fortsette. |
| `bekreftelse_tilgjengelig_hendelser_per_dag_by_gjelder_til` ([Grafana](#bekreftelse_tilgjengelig_hendelser_per_dag_by_gjelder_til), [SQL](src/main/resources/materialized_views/bekreftelse_tilgjengelig_hendelser_per_dag_by_gjelder_til.sql)) | Antall `bekreftelse.tilgjengelig`-hendelser per dag og datoen bekreftelsen gjelder til. |
| `forste_bekreftelse_som_korttid_indikator` ([Grafana](#forste_bekreftelse_som_korttid_indikator), [SQL](src/main/resources/materialized_views/forste_bekreftelse_som_korttid_indikator.sql)) | Avsluttede perioder fordelt på startmåned, svar om arbeid i første bekreftelse og periodens varighetsintervall. |
| `leverte_bekreftelser_sammenlignet_med_tidligere_uker` ([Grafana](#leverte_bekreftelser_sammenlignet_med_tidligere_uker), [SQL](src/main/resources/materialized_views/leverte_bekreftelser_sammenlignet_med_tidligere_uker.sql)) | Daglige leverte bekreftelser per løsning sammenlignet med samme ukedag de fire foregående ukene. |
| `sist_leverte_bekreftelse_aktive_perioder_by_gjelder_til_loesning` ([Grafana](#sist_leverte_bekreftelse_aktive_perioder_by_gjelder_til_loesning), [SQL](src/main/resources/materialized_views/sist_leverte_bekreftelse_aktive_perioder_by_gjelder_til_loesning.sql)) | Aktive perioder fordelt på sluttdato og løsning i periodens sist leverte bekreftelse. |
| `tilbakevending_etter_siste_bekreftelse_jobb` ([Grafana](#tilbakevending_etter_siste_bekreftelse_jobb), [SQL](src/main/resources/materialized_views/tilbakevending_etter_siste_bekreftelse_jobb.sql)) | Avsluttede perioder fordelt på siste jobbsvar og hvor lang tid det tok før neste periode startet. |

#### Avvisninger og datakvalitet

| View | Innhold |
| --- | --- |
| `over_18_aar_forsinkelse_til_start_over_tid` ([Grafana](#over_18_aar_forsinkelse_til_start_over_tid), [SQL](src/main/resources/materialized_views/over_18_aar_forsinkelse_til_start_over_tid.sql)) | Månedlig ventetid fra første avvisning til første start for personer over 18 år, inkludert manglende starter og persentiler. |
| `paavegnav_mottatt_uten_aktiv_periode` ([Grafana](#paavegnav_mottatt_uten_aktiv_periode), [SQL](src/main/resources/materialized_views/paavegnav_mottatt_uten_aktiv_periode.sql)) | Antall på-vegne-av-meldinger mottatt uten en aktiv periode, sammen med totalt antall meldinger per dag, løsning og handling. |
| `under_18_aar_forsinkelse_til_start` ([Grafana](#under_18_aar_forsinkelse_til_start), [SQL](src/main/resources/materialized_views/under_18_aar_forsinkelse_til_start.sql)) | Ventetid fra første avvisning til første start for mindreårige, fordelt på intervaller og om personen fortsatt var under 18 ved start. |
| `under_18_aar_forsinkelse_til_start_over_tid` ([Grafana](#under_18_aar_forsinkelse_til_start_over_tid), [SQL](src/main/resources/materialized_views/under_18_aar_forsinkelse_til_start_over_tid.sql)) | Månedlig ventetid fra første avvisning til første start for mindreårige, inkludert manglende starter og persentiler. |
| `unike_avvist_per_maaned` ([Grafana](#unike_avvist_per_maaned), [SQL](src/main/resources/materialized_views/unike_avvist_per_maaned.sql)) | Unike avvisningshendelser per måned, fordelt på egenskaper registrert sammen med avvisningen. |

## Bruk i Grafana

Viewene i `arbeidssoekerregisteret_grafana` kan brukes med BigQuery-datakilden i
Grafana. Velg **Time series** som spørringsformat og **Code** for å skrive SQL.
Bruk fullt kvalifiserte tabellnavn med prosjektet `paw-prod-7151`. Uten
prosjekt-ID leter Grafana i datakildens standardprosjekt, der viewene ikke
finnes.

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

Alle tallkolonner blir egne serier. BigQuery-datakilden tolker ikke vilkårlige
tekstkolonner automatisk som serienavn. Filtrer derfor på tekstkolonner, eller
bruk betinget aggregering for å gjøre hver tekstverdi til en egen tallkolonne.
For `loesning` kan du bruke et SQL-filter eller en dashboardvariabel og vise
løsningene i separate paneler.

De tre nye viewene som bruker bekreftelser, avgrenser bekreftelsesdata til datoer
fra og med 1. september 2025. Det finnes ingen data før mars 2025, og
datakvaliteten frem til og med august 2025 er ikke god nok for disse viewene.

<a id="periodestarter_sammenlignet_med_tidligere_uker"></a>

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
    antall_periodestarter AS denne_uken,
    antall_1_uke_siden,
    antall_2_uker_siden,
    antall_3_uker_siden,
    antall_4_uker_siden
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.periodestarter_sammenlignet_med_tidligere_uker`
WHERE $__timeFilter(TIMESTAMP(dag, 'Europe/Oslo'))
  AND dag < CURRENT_DATE('Europe/Oslo')
ORDER BY dag
```

Viewet eksponerer `dag` som `DATE`, lik de andre viewene. Grafana-spørringen
konverterer datoen til et `TIMESTAMP` satt til midnatt i `Europe/Oslo`, slik at
Grafana kan bruke resultatet som tidsakse og med `$__timeFilter`. Tallene viser
vellykkede periodestarter. Et negativt avvik er en indikator på mulig bortfall
under en driftshendelse, ikke et eksakt antall brukere som ble hindret.

<a id="periodeavslutninger_sammenlignet_med_tidligere_uker"></a>

### Periodeavslutninger sammenlignet med tidligere uker

Viewet `arbeidssoekerregisteret_grafana.periodeavslutninger_sammenlignet_med_tidligere_uker`
har samme struktur som viewet for periodestarter, men teller perioder gruppert på
`avsluttet.tidspunkt`. Hver periode telles én gang.

```sql
SELECT
    TIMESTAMP(dag, 'Europe/Oslo') AS time,
    antall_periodeavslutninger AS denne_uken,
    antall_1_uke_siden,
    antall_2_uker_siden,
    antall_3_uker_siden,
    antall_4_uker_siden
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.periodeavslutninger_sammenlignet_med_tidligere_uker`
WHERE $__timeFilter(TIMESTAMP(dag, 'Europe/Oslo'))
  AND dag < CURRENT_DATE('Europe/Oslo')
ORDER BY dag
```

<a id="leverte_bekreftelser_sammenlignet_med_tidligere_uker"></a>

### Leverte bekreftelser sammenlignet med tidligere uker

Viewet `arbeidssoekerregisteret_grafana.leverte_bekreftelser_sammenlignet_med_tidligere_uker`
teller leverte bekreftelser per `tidspunkt` og `loesning`. Det følger samme
ukestruktur som periode-viewene, med løsning som en ekstra dimensjon. Bruk
**Table** for å vise alle løsninger. For **Time series** må du filtrere på én
løsning, for eksempel med en enkeltvalgsvariabel i dashboardet.

```sql
SELECT
    TIMESTAMP(dag, 'Europe/Oslo') AS time,
    loesning,
    antall_leverte_bekreftelser AS denne_uken,
    antall_1_uke_siden,
    antall_2_uker_siden,
    antall_3_uker_siden,
    antall_4_uker_siden
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.leverte_bekreftelser_sammenlignet_med_tidligere_uker`
WHERE $__timeFilter(TIMESTAMP(dag, 'Europe/Oslo'))
  AND dag < CURRENT_DATE('Europe/Oslo')
ORDER BY dag, loesning
```

<a id="aktive_perioder_by_har_jobbet_dag_for_dag"></a>

### Aktive perioder etter siste svar om arbeid

Viewet `arbeidssoekerregisteret_grafana.aktive_perioder_by_har_jobbet_dag_for_dag`
viser antall aktive perioder per dag fordelt på:

- `JA`: Den siste leverte bekreftelsen som dekker dagen, oppga at personen hadde
  jobbet.
- `NEI`: Den siste leverte bekreftelsen som dekker dagen, oppga at personen ikke
  hadde jobbet.
- `UKJENT`: Dagen er ikke dekket av en levert bekreftelse.

En bekreftelse gjelder fra og med `gjelder_fra` til og med `gjelder_til`.
Når en bekreftelse leveres på etterskudd, oppdateres derfor de historiske
dagene den dekker. Ved overlappende eller korrigerte bekreftelser brukes den
senest leverte per dag.

Perioden regnes som aktiv fra og med `startet.tidspunkt` og frem til, men ikke
med, `avsluttet.tidspunkt`. Når sluttdatoen mottas, fjerner neste oppfriskning
perioden fra alle dager fra og med sluttdatoen.

```sql
SELECT
    TIMESTAMP(dag, 'Europe/Oslo') AS time,
    SUM(IF(har_jobbet = 'JA', antall_aktive_perioder, 0)) AS ja,
    SUM(IF(har_jobbet = 'NEI', antall_aktive_perioder, 0)) AS nei,
    SUM(IF(har_jobbet = 'UKJENT', antall_aktive_perioder, 0)) AS ukjent
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.aktive_perioder_by_har_jobbet_dag_for_dag`
WHERE $__timeFilter(TIMESTAMP(dag, 'Europe/Oslo'))
  AND dag < CURRENT_DATE('Europe/Oslo')
GROUP BY dag
ORDER BY dag
```

Den betingede aggregeringen gjør `JA`, `NEI` og `UKJENT` til egne tallkolonner,
slik at Grafana viser dem som separate serier.

<a id="avsluttede_perioder_andel_har_jobbet_per_dag"></a>

### Avslutninger og arbeid i de siste bekreftelsene

Viewet `arbeidssoekerregisteret_grafana.avsluttede_perioder_andel_har_jobbet_per_dag`
viser antall avsluttede arbeidssøkerperioder per dag. Det viser også
gjennomsnittlig andel `har_jobbet = true` i hver periodes siste 2, 10 og 20
bekreftelser.

Andelen beregnes først for hver periode. Dagsverdien er deretter et uvektet
gjennomsnitt av periodene som ble avsluttet den dagen. En periode må ha minst
henholdsvis 2, 10 eller 20 bekreftelser for å inngå i den aktuelle andelen.
Kolonnene `antall_perioder_med_2_bekreftelser`,
`antall_perioder_med_10_bekreftelser` og
`antall_perioder_med_20_bekreftelser` viser datagrunnlaget for hver verdi.

Ved korrigerte bekreftelser for samme `gjelder_fra`–`gjelder_til` brukes den
senest leverte. Bekreftelsene rangeres deretter etter `gjelder_til`, slik at
andelene kan oppdateres bakover i tid.

```sql
SELECT
    TIMESTAMP(dag, 'Europe/Oslo') AS time,
    gjennomsnittlig_andel_har_jobbet_siste_2 AS siste_2,
    gjennomsnittlig_andel_har_jobbet_siste_10 AS siste_10,
    gjennomsnittlig_andel_har_jobbet_siste_20 AS siste_20
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.avsluttede_perioder_andel_har_jobbet_per_dag`
WHERE $__timeFilter(TIMESTAMP(dag, 'Europe/Oslo'))
  AND dag < CURRENT_DATE('Europe/Oslo')
ORDER BY dag
```

Sett enheten til **Percent (0.0–1.0)**. Vis antall avsluttede perioder og
datagrunnlaget for andelene i et eget panel:

```sql
SELECT
    TIMESTAMP(dag, 'Europe/Oslo') AS time,
    antall_avsluttede_perioder,
    antall_perioder_med_2_bekreftelser,
    antall_perioder_med_10_bekreftelser,
    antall_perioder_med_20_bekreftelser
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.avsluttede_perioder_andel_har_jobbet_per_dag`
WHERE $__timeFilter(TIMESTAMP(dag, 'Europe/Oslo'))
  AND dag < CURRENT_DATE('Europe/Oslo')
ORDER BY dag
```

<a id="avsluttede_perioder_sammenhengende_har_jobbet_per_uke"></a>

### Sammenhengende positive jobbsvar ved avslutning

Viewet
`arbeidssoekerregisteret_grafana.avsluttede_perioder_sammenhengende_har_jobbet_per_uke`
fordeler avsluttede arbeidssøkerperioder etter hvor mange sammenhengende
bekreftelser med `har_jobbet = true` perioden hadde ved avslutning.
Bekreftelsene leses bakover fra den nyeste, og rekken stopper ved første
`har_jobbet = false`.

En periode der siste bekreftelse har `har_jobbet = false`, havner i bøtte `0`.
En periode med de nyeste svarene `JA, JA, JA, NEI, JA` havner i bøtte `3`.
Rekker på 10 eller flere samles i `10+`. Perioder uten bekreftelser ligger i en
egen bøtte.

Viewet har én rad per ISO-uke fra mandag til søndag. Kolonnen `uke_start`
identifiserer uken, mens `iso_aar` og `iso_uke` kan brukes som merkelapper.
Hver bøtte finnes både som `antall_*` og `andel_*`. Alle andeler bruker
`antall_avsluttede_perioder`, inkludert perioder uten bekreftelser, som nevner.
Dermed summerer bøtteantallene til uketotalen og bøtteandelene til 1.

Viewet tar med perioder som ble avsluttet fra og med 1. september 2025, og
bruker hele bekreftelseshistorikken til disse periodene når rekken beregnes.
Cutoff-datoen er en mandag, slik at den første raden dekker en full ISO-uke.
Ved flere svar for samme `gjelder_fra`–`gjelder_til` brukes det senest
innsendte svaret.

Eksempel for et stablet panel med antall:

```sql
SELECT
    TIMESTAMP(uke_start, 'Europe/Oslo') AS time,
    antall_ingen_bekreftelser,
    antall_sammenhengende_ja_0,
    antall_sammenhengende_ja_1,
    antall_sammenhengende_ja_2,
    antall_sammenhengende_ja_3,
    antall_sammenhengende_ja_4,
    antall_sammenhengende_ja_5,
    antall_sammenhengende_ja_6,
    antall_sammenhengende_ja_7,
    antall_sammenhengende_ja_8,
    antall_sammenhengende_ja_9,
    antall_sammenhengende_ja_10_pluss
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.avsluttede_perioder_sammenhengende_har_jobbet_per_uke`
WHERE $__timeFilter(TIMESTAMP(uke_start, 'Europe/Oslo'))
  AND uke_start < DATE_TRUNC(CURRENT_DATE('Europe/Oslo'), ISOWEEK)
ORDER BY uke_start
```

Filteret utelater inneværende, ufullstendige uke. Viewet eksponerer bare
ukentlige aggregater i et datasett med bredere tilgang. Basetabellene ligger i
et teambegrenset datasett, og brukere som kan koble aggregatene til rådata, har
allerede tilgang til rådataene.

Personvernrisikoen vurderes ut fra totalt antall avslutninger i uken, ikke
antallet i hver bøtte. En liten bøtte identifiserer ikke en periode når ukens
samlede populasjon er stor og viewet mangler andre dimensjoner. Vurder
ukesbasert skjerming og tilgang på nytt dersom uketotalen blir lav, eller hvis
det legges til dimensjoner som avslutningsårsak, løsning, geografi eller
brukergruppe.

## Flere Grafana-spørringer

Oppskriftene under dekker viewene som ikke er beskrevet i de foregående
eksemplene. Velg **Table** når resultatet har tekstdimensjoner som varierer
dynamisk. Bruk eventuelt Grafana-transformasjoner eller dashboardvariabler for å
filtrere én dimensjonsverdi før resultatet vises som tidsserie.

<a id="aktive_by_weeks_since_start"></a>

### Aktive perioder per uke siden start

Bruk **Table** eller **Heatmap**. Resultatet viser antall aktive perioder per dag
og hele uker siden periodestart.

```sql
SELECT
    TIMESTAMP(day, 'Europe/Oslo') AS time,
    weeks_since_start,
    active_count
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.aktive_by_weeks_since_start`
WHERE $__timeFilter(TIMESTAMP(day, 'Europe/Oslo'))
  AND day < CURRENT_DATE('Europe/Oslo')
ORDER BY day, weeks_since_start
```

<a id="aktive_dag_for_dag"></a>

### Aktive perioder per dag

Bruk **Time series**. Panelet viser samlet antall aktive perioder.

```sql
SELECT
    TIMESTAMP(day, 'Europe/Oslo') AS time,
    active_count
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.aktive_dag_for_dag`
WHERE $__timeFilter(TIMESTAMP(day, 'Europe/Oslo'))
  AND day < CURRENT_DATE('Europe/Oslo')
ORDER BY day
```

<a id="aktive_duration_stats_per_month"></a>

### Varighet for aktive perioder per måned

Bruk **Time series**. Spørringen viser median og høyeste antall aktive uker ved
starten av hver måned. Viewet har også antall perioder i faste
varighetsintervaller, som kan brukes i et eget stablet panel.

```sql
SELECT
    TIMESTAMP(month_start, 'Europe/Oslo') AS time,
    median_weeks_active,
    max_weeks_active
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.aktive_duration_stats_per_month`
WHERE $__timeFilter(TIMESTAMP(month_start, 'Europe/Oslo'))
  AND month_start <= CURRENT_DATE('Europe/Oslo')
ORDER BY month_start
```

<a id="alle_perioder"></a>

### Alle perioder

Bruk **Table** for å kontrollere start- og sluttdato for periodene. Viewet
inneholder ikke identifikatorer og egner seg ikke til oppslag på enkeltpersoner.

```sql
SELECT
    startet.tidspunkt AS startet,
    avsluttet.tidspunkt AS avsluttet,
    startet.brukertype AS startet_av,
    avsluttet.aarsak AS avsluttet_aarsak
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.alle_perioder`
WHERE $__timeFilter(TIMESTAMP(startet.tidspunkt, 'Europe/Oslo'))
ORDER BY startet.tidspunkt DESC
```

Bruk **Time series** for å vise antall perioder startet per dag. Sett manglende
verdier til `0` i Grafana hvis panelet skal vise dager uten periodestarter:

```sql
SELECT
    TIMESTAMP(startet.tidspunkt, 'Europe/Oslo') AS time,
    COUNT(*) AS antall_periodestarter
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.alle_perioder`
WHERE $__timeFilter(TIMESTAMP(startet.tidspunkt, 'Europe/Oslo'))
  AND startet.tidspunkt < CURRENT_DATE('Europe/Oslo')
GROUP BY startet.tidspunkt
ORDER BY startet.tidspunkt
```

Den tilsvarende spørringen for antall avsluttede perioder per dag bruker
`avsluttet.tidspunkt`:

```sql
SELECT
    TIMESTAMP(avsluttet.tidspunkt, 'Europe/Oslo') AS time,
    COUNT(*) AS antall_periodeavslutninger
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.alle_perioder`
WHERE avsluttet IS NOT NULL
  AND $__timeFilter(TIMESTAMP(avsluttet.tidspunkt, 'Europe/Oslo'))
  AND avsluttet.tidspunkt < CURRENT_DATE('Europe/Oslo')
GROUP BY avsluttet.tidspunkt
ORDER BY avsluttet.tidspunkt
```

<a id="avsluttede_perioder_varighet_distribusjon_per_maaned"></a>

### Varighetsfordeling for avsluttede perioder

Bruk **Table** eller **Bar chart**. Resultatet kan filtreres på
`avsluttet_aarsak` og viser antall i hvert varighetsintervall.

```sql
SELECT
    TIMESTAMP(PARSE_DATE('%Y-%m', avsluttet_maaned), 'Europe/Oslo') AS time,
    avsluttet_aarsak,
    varighet_bucket,
    antall
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.avsluttede_perioder_varighet_distribusjon_per_maaned`
WHERE $__timeFilter(
    TIMESTAMP(PARSE_DATE('%Y-%m', avsluttet_maaned), 'Europe/Oslo')
)
ORDER BY time, avsluttet_aarsak, varighet_bucket
```

<a id="avsluttede_perioder_varighet_stats_per_maaned"></a>

### Varighetsstatistikk for avsluttede perioder

Bruk **Table**. Resultatet viser antall, gjennomsnitt og persentiler per måned og
avslutningsårsak. Filtrer på én årsak før du bruker resultatet som tidsserie.

```sql
SELECT
    TIMESTAMP(PARSE_DATE('%Y-%m', avsluttet_maaned), 'Europe/Oslo') AS time,
    avsluttet_aarsak,
    antall_avsluttet,
    gjennomsnitt_dager,
    p25_dager,
    median_dager,
    p75_dager,
    p90_dager,
    antall_korte_perioder,
    antall_medium_perioder,
    antall_lange_perioder
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.avsluttede_perioder_varighet_stats_per_maaned`
WHERE $__timeFilter(
    TIMESTAMP(PARSE_DATE('%Y-%m', avsluttet_maaned), 'Europe/Oslo')
)
ORDER BY time, avsluttet_aarsak
```

<a id="avsluttet_by_aarsak"></a>

### Avslutninger etter årsak

Bruk **Table** eller **Bar chart**. Spørringen viser daglige avslutninger per
årsak.

```sql
SELECT
    TIMESTAMP(tidspunkt, 'Europe/Oslo') AS time,
    aarsak,
    antall_avsluttet
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.avsluttet_by_aarsak`
WHERE $__timeFilter(TIMESTAMP(tidspunkt, 'Europe/Oslo'))
  AND tidspunkt < CURRENT_DATE('Europe/Oslo')
ORDER BY tidspunkt, aarsak
```

<a id="avsluttet_by_aarsak_brukertype"></a>

### Avslutninger etter årsak og brukertype

Bruk **Table** eller **Bar chart**. Resultatet deler de daglige avslutningene på
både årsak og brukertype.

```sql
SELECT
    TIMESTAMP(tidspunkt, 'Europe/Oslo') AS time,
    aarsak,
    brukertype,
    antall_avsluttet
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.avsluttet_by_aarsak_brukertype`
WHERE $__timeFilter(TIMESTAMP(tidspunkt, 'Europe/Oslo'))
  AND tidspunkt < CURRENT_DATE('Europe/Oslo')
ORDER BY tidspunkt, aarsak, brukertype
```

<a id="korttid_langtid_opplysninger_ved_start"></a>

### Korttids- og langtidsperioder etter opplysninger ved start

Bruk **Table**. Viewet har ikke en tidsdimensjon, men sammenligner korte og lange
perioder for kombinasjoner av brukertype og opplysninger registrert ved start.
Sett `andel_korttid` til enheten **Percent (0.0–1.0)**.

```sql
SELECT
    brukertype,
    er_norsk,
    er_eu_eoes,
    bosatt_etter_freg,
    dnummer,
    siste_inn_til_norge,
    har_oppholdstillatelse,
    er_gjentakende_registrant,
    antall,
    antall_korttid,
    antall_langtid,
    andel_korttid
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.korttid_langtid_opplysninger_ved_start`
ORDER BY antall DESC
```

<a id="startet_by_tid_siden_avsluttet_aarsak"></a>

### Nye perioder etter tid siden forrige avslutning

Bruk **Table** eller **Bar chart**. Spørringen grupperer periodestarter etter
brukertype, forrige avslutningsårsak og tid siden forrige avslutning.

```sql
SELECT
    TIMESTAMP(startet_tidspunkt, 'Europe/Oslo') AS time,
    startet_brukertype,
    previous_avsluttet_aarsak,
    time_since_last_avsluttet_bucket,
    COUNT(*) AS antall
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.startet_by_tid_siden_avsluttet_aarsak`
WHERE $__timeFilter(TIMESTAMP(startet_tidspunkt, 'Europe/Oslo'))
GROUP BY
    startet_tidspunkt,
    startet_brukertype,
    previous_avsluttet_aarsak,
    time_since_last_avsluttet_bucket
ORDER BY startet_tidspunkt, time_since_last_avsluttet_bucket
```

<a id="tilbakevending_etter_avsluttet_per_maaned"></a>

### Tilbakevending etter avsluttet periode

Bruk **Table** eller **Bar chart**. Resultatet viser hvor raskt en ny periode
startet etter avslutning, fordelt på avslutningsmåned og årsak.

```sql
SELECT
    TIMESTAMP(PARSE_DATE('%Y-%m', avsluttet_maaned), 'Europe/Oslo') AS time,
    avsluttet_aarsak,
    tid_til_retur_bucket,
    antall
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.tilbakevending_etter_avsluttet_per_maaned`
WHERE $__timeFilter(
    TIMESTAMP(PARSE_DATE('%Y-%m', avsluttet_maaned), 'Europe/Oslo')
)
ORDER BY time, avsluttet_aarsak, tid_til_retur_bucket
```

<a id="bekreftelse_jobb_og_fortsettelse_per_maaned"></a>

### Arbeid og fortsettelse i bekreftelser

Bruk **Table**. Resultatet viser månedlige antall og andeler per løsning og
brukertype. Sett andelskolonnene til **Percent (0.0–1.0)**.

```sql
SELECT
    TIMESTAMP(PARSE_DATE('%Y-%m', maaned), 'Europe/Oslo') AS time,
    loesning,
    brukertype,
    antall_bekreftelses,
    antall_har_jobbet,
    antall_vil_ikke_fortsette,
    antall_har_jobb_og_slutter,
    antall_ingen_jobb_og_slutter,
    andel_har_jobbet,
    andel_vil_ikke_fortsette
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.bekreftelse_jobb_og_fortsettelse_per_maaned`
WHERE $__timeFilter(TIMESTAMP(PARSE_DATE('%Y-%m', maaned), 'Europe/Oslo'))
ORDER BY time, loesning, brukertype
```

<a id="bekreftelse_tilgjengelig_hendelser_per_dag_by_gjelder_til"></a>

### Tilgjengelige bekreftelser etter frist

Bruk **Table** eller **Heatmap**. Resultatet viser når
`bekreftelse.tilgjengelig` ble sendt, hvilken sluttdato den gjaldt og antallet.

```sql
SELECT
    TIMESTAMP(dag, 'Europe/Oslo') AS time,
    gjelder_til,
    antall_hendelser
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.bekreftelse_tilgjengelig_hendelser_per_dag_by_gjelder_til`
WHERE $__timeFilter(TIMESTAMP(dag, 'Europe/Oslo'))
ORDER BY dag, gjelder_til
```

<a id="forste_bekreftelse_som_korttid_indikator"></a>

### Første bekreftelse som indikator på varighet

Bruk **Table** eller **Bar chart**. Resultatet sammenligner svaret om arbeid i
første bekreftelse med periodens endelige varighet.

```sql
SELECT
    TIMESTAMP(PARSE_DATE('%Y-%m', start_maaned), 'Europe/Oslo') AS time,
    forste_bekreftelse_status,
    varighet_bucket,
    antall
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.forste_bekreftelse_som_korttid_indikator`
WHERE $__timeFilter(TIMESTAMP(PARSE_DATE('%Y-%m', start_maaned), 'Europe/Oslo'))
ORDER BY time, forste_bekreftelse_status, varighet_bucket
```

<a id="sist_leverte_bekreftelse_aktive_perioder_by_gjelder_til_loesning"></a>

### Siste bekreftelse for aktive perioder

Bruk **Table** eller **Bar chart**. Resultatet viser aktive perioder etter
sluttdato og løsning i den sist leverte bekreftelsen.

```sql
SELECT
    TIMESTAMP(gjelder_til, 'Europe/Oslo') AS time,
    loesning,
    active_count
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.sist_leverte_bekreftelse_aktive_perioder_by_gjelder_til_loesning`
WHERE $__timeFilter(TIMESTAMP(gjelder_til, 'Europe/Oslo'))
ORDER BY gjelder_til, loesning
```

<a id="tilbakevending_etter_siste_bekreftelse_jobb"></a>

### Tilbakevending etter siste svar om arbeid

Bruk **Table** eller **Bar chart**. Resultatet sammenligner siste jobbsvar før
avslutning med hvor lang tid det tok før neste periode startet.

```sql
SELECT
    TIMESTAMP(PARSE_DATE('%Y-%m', avsluttet_maaned), 'Europe/Oslo') AS time,
    siste_bekreftelse_jobb_status,
    tid_til_retur_bucket,
    antall
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.tilbakevending_etter_siste_bekreftelse_jobb`
WHERE $__timeFilter(
    TIMESTAMP(PARSE_DATE('%Y-%m', avsluttet_maaned), 'Europe/Oslo')
)
ORDER BY time, siste_bekreftelse_jobb_status, tid_til_retur_bucket
```

<a id="over_18_aar_forsinkelse_til_start_over_tid"></a>

### Forsinkelse til start for personer over 18 år

Bruk **Time series**. Spørringen viser gjennomsnitt, median og 90-persentil for
antall dager fra første avvisning til første start. Bruk
`missing_percentage` fra viewet i et eget prosentpanel for andelen som ikke har
startet.

```sql
SELECT
    TIMESTAMP(PARSE_DATE('%Y-%m', month_bucket), 'Europe/Oslo') AS time,
    avg_latency_days,
    median_latency_days,
    p90_latency_days
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.over_18_aar_forsinkelse_til_start_over_tid`
WHERE $__timeFilter(TIMESTAMP(PARSE_DATE('%Y-%m', month_bucket), 'Europe/Oslo'))
ORDER BY time
```

<a id="paavegnav_mottatt_uten_aktiv_periode"></a>

### På-vegne-av mottatt uten aktiv periode

Bruk **Table**. Sammenlign `antall_mottatt_uten_aktiv_periode` med
`total_antall` per dag, løsning, handling og friststatus.

```sql
SELECT
    TIMESTAMP(tidspunkt, 'Europe/Oslo') AS time,
    loesning,
    handling,
    frist_brutt,
    antall_mottatt_uten_aktiv_periode,
    total_antall
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.paavegnav_mottatt_uten_aktiv_periode`
WHERE $__timeFilter(TIMESTAMP(tidspunkt, 'Europe/Oslo'))
ORDER BY tidspunkt, loesning, handling
```

<a id="under_18_aar_forsinkelse_til_start"></a>

### Fordeling av forsinkelse til start for mindreårige

Bruk **Table** eller **Bar chart**. Viewet har ikke en tidsdimensjon og viser
antall samt minimum, maksimum og gjennomsnittlig ventetid per intervall.

```sql
SELECT
    under_18_ved_start,
    latency_bucket,
    count,
    min_days,
    max_days,
    avg_days
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.under_18_aar_forsinkelse_til_start`
ORDER BY under_18_ved_start, min_days
```

<a id="under_18_aar_forsinkelse_til_start_over_tid"></a>

### Forsinkelse til start for mindreårige over tid

Bruk **Time series**. Spørringen viser gjennomsnitt, median og 90-persentil for
antall dager fra første avvisning til første start. Vis
`missing_percentage` i et eget prosentpanel.

```sql
SELECT
    TIMESTAMP(PARSE_DATE('%Y-%m', month_bucket), 'Europe/Oslo') AS time,
    avg_latency_days,
    median_latency_days,
    p90_latency_days
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.under_18_aar_forsinkelse_til_start_over_tid`
WHERE $__timeFilter(TIMESTAMP(PARSE_DATE('%Y-%m', month_bucket), 'Europe/Oslo'))
ORDER BY time
```

<a id="unike_avvist_per_maaned"></a>

### Unike avvisninger per måned

Bruk **Table**. Resultatet viser antall unike avviste identifikatorer fordelt på
egenskapene som fulgte avvisningshendelsen.

```sql
SELECT
    TIMESTAMP(DATE(year, month, 1), 'Europe/Oslo') AS time,
    under_18_aar,
    er_norsk,
    ikke_eu,
    dnummer,
    doed,
    savnet,
    opphoert_id,
    distinct_id_count
FROM `paw-prod-7151.arbeidssoekerregisteret_grafana.unike_avvist_per_maaned`
WHERE $__timeFilter(TIMESTAMP(DATE(year, month, 1), 'Europe/Oslo'))
ORDER BY time
```