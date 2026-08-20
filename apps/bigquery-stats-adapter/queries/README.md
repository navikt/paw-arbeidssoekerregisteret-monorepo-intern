# Manuelle BigQuery-spørringer

Spørringene i denne mappen kjøres manuelt i BigQuery-konsollen. Applikasjonen leser dem ikke ved oppstart.

SQL under `src/main/resources/materialized_views/` har et annet formål: Applikasjonen bruker disse filene til å opprette materialiserte views i BigQuery.

## Spørringer

- `profilering-permitterte.sql` analyserer profileringen av personer som var permittert da arbeidssøkerperioden startet.
- `aggregert-profileringsgrunnlag.sql` lager et aggregert analysegrunnlag for alle arbeidssøkersekvenser. Resultatet kan eksporteres som CSV eller JSON for videre analyse.

Den aggregerte spørringen slår sammen perioder for samme person når oppholdet mellom dem er høyst sju dager. Sammenslåingen er transitiv, så A→B og B→C blir én sekvens fra starten av A til slutten av C. Initial status og profilering hentes fra den første perioden.

Resultatet grupperes etter startperiode, initial status, profilering, aldersgruppe, NUS-nivå, om utdanningen er bestått og godkjent, hindringer, arbeid siste 12 måneder og STYRK-08 nivå 2. Det inneholder antall observerbare og avsluttede sekvenser etter 30, 90, 180 og 365 dager.

`startintervall` øverst i `aggregert-profileringsgrunnlag.sql` styrer tidsgrupperingen. Gyldige verdier er `maaned`, `kvartal`, `halvaar` og `aar`. Standardverdien er `halvaar`. Grovere intervall gir flere synlige grupper uten å redusere `minstegruppe`.

Spørringen returnerer bare disjunkte grupper med minst 100 arbeidssøkersekvenser. Eksporter aldri interne CTE-er eller individrader til analyseverktøy.
