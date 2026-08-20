-- Aggregert analysegrunnlag for alle arbeidssokersekvenser.
-- Hele filen kan kjoeres som ett BigQuery-script.
-- Resultatet inneholder ingen person-, periode- eller opplysnings-ID-er.

DECLARE kohort_fra DATE DEFAULT DATE '2025-01-01';
DECLARE maks_opphold_dager INT64 DEFAULT 7;
DECLARE minstegruppe INT64 DEFAULT 100;
-- Gyldige verdier: maaned, kvartal, halvaar, aar.
DECLARE startintervall STRING DEFAULT 'aar';
DECLARE analysedato DATE DEFAULT CURRENT_DATE('Europe/Oslo');

WITH perioder_aggregert AS (
    SELECT
        correlation_id,
        MIN(startet.tidspunkt) AS startdato,
        MAX(avsluttet.tidspunkt) AS sluttdato
    FROM `arbeidssoekerregisteret_internt.perioder`
    GROUP BY correlation_id
),
person_per_periode AS (
    SELECT
        correlation_id,
        ANY_VALUE(id) AS person_id
    FROM `arbeidssoekerregisteret_internt.hendelser`
    WHERE type = 'intern.v1.startet'
    GROUP BY correlation_id
    HAVING COUNT(DISTINCT id) = 1
),
perioder_med_person AS (
    SELECT
        p.correlation_id,
        h.person_id,
        p.startdato,
        p.sluttdato
    FROM perioder_aggregert p
    INNER JOIN person_per_periode h USING (correlation_id)
    WHERE p.sluttdato IS NULL OR p.sluttdato >= p.startdato
),
perioder_med_forrige_dekning AS (
    SELECT
        *,
        MAX(COALESCE(sluttdato, DATE '9999-12-31')) OVER (
            PARTITION BY person_id
            ORDER BY startdato, correlation_id
            ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
        ) AS forrige_dekning_til
    FROM perioder_med_person
),
perioder_med_sekvensbrudd AS (
    SELECT
        *,
        CASE
            WHEN forrige_dekning_til IS NULL THEN 1
            WHEN forrige_dekning_til = DATE '9999-12-31' THEN 0
            WHEN startdato > DATE_ADD(
                    forrige_dekning_til,
                    INTERVAL maks_opphold_dager DAY
                ) THEN 1
            ELSE 0
        END AS sekvensbrudd
    FROM perioder_med_forrige_dekning
),
perioder_med_sekvensnummer AS (
    SELECT
        *,
        SUM(sekvensbrudd) OVER (
            PARTITION BY person_id
            ORDER BY startdato, correlation_id
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        ) AS sekvensnummer
    FROM perioder_med_sekvensbrudd
),
sekvenser AS (
    SELECT
        person_id,
        sekvensnummer,
        MIN(startdato) AS startdato,
        IF(
            COUNTIF(sluttdato IS NULL) > 0,
            NULL,
            MAX(sluttdato)
        ) AS sluttdato,
        (
            ARRAY_AGG(
                correlation_id
                ORDER BY startdato, correlation_id
                LIMIT 1
            )
        )[OFFSET(0)] AS forste_correlation_id
    FROM perioder_med_sekvensnummer
    GROUP BY person_id, sekvensnummer
),
sekvenser_i_kohort AS (
    SELECT *
    FROM sekvenser
    WHERE startdato >= kohort_fra
      -- En nylig avsluttet sekvens kan fortsatt bli forlenget.
      AND (
          sluttdato IS NULL
          OR sluttdato <= DATE_SUB(
              analysedato,
              INTERVAL maks_opphold_dager DAY
          )
      )
),
sekvensgrunnlag AS (
    SELECT
        s.person_id,
        s.sekvensnummer,
        s.startdato,
        s.sluttdato,
        o.opplysninger_id,
        o.utdanning.nus AS utdanning_nus,
        o.utdanning.bestaatt AS utdanning_bestaatt,
        o.utdanning.godkjent AS utdanning_godkjent,
        o.helse.helsetilstand_hindrer_arbeid AS helse_hindrer,
        o.annet.andre_forhold_hindrer_arbeid AS andre_forhold_hindrer,
        o.jobbsituasjoner,
        p.profilert_til,
        p.aldersgruppe,
        p.jobbet_sammenhengende_seks_av_tolv_siste_mnd
    FROM sekvenser_i_kohort s
    LEFT JOIN `arbeidssoekerregisteret_internt.opplysninger` o
        ON o.correlation_id = s.forste_correlation_id
        AND o.sendt_inn_av.tidspunkt = s.startdato
    LEFT JOIN `arbeidssoekerregisteret_internt.profilering` p
        ON p.correlation_id = s.forste_correlation_id
        AND p.opplysninger_id = o.opplysninger_id
),
initial_jobbsituasjon AS (
    SELECT
        g.person_id,
        g.sekvensnummer,
        COALESCE(
            STRING_AGG(
                DISTINCT j.beskrivelse,
                '|'
                ORDER BY j.beskrivelse
            ),
            'ikke_oppgitt'
        ) AS initial_status_set,
        COALESCE(
            STRING_AGG(
                DISTINCT IF(
                    REGEXP_CONTAINS(j.stilling_styrk08, r'^[0-9]{4}$'),
                    SUBSTR(j.stilling_styrk08, 1, 2),
                    NULL
                ),
                '|'
                ORDER BY IF(
                    REGEXP_CONTAINS(j.stilling_styrk08, r'^[0-9]{4}$'),
                    SUBSTR(j.stilling_styrk08, 1, 2),
                    NULL
                )
            ),
            'ikke_oppgitt_eller_ugyldig'
        ) AS styrk08_nivaa_2
    FROM sekvensgrunnlag g
    LEFT JOIN UNNEST(g.jobbsituasjoner) j
    GROUP BY g.person_id, g.sekvensnummer
),
attributter_per_sekvens AS (
    SELECT
        person_id,
        sekvensnummer,
        MIN(startdato) AS startdato,
        MAX(sluttdato) AS sluttdato,
        CASE
            WHEN COUNT(DISTINCT utdanning_nus) = 0 THEN 'ikke_oppgitt'
            WHEN COUNT(DISTINCT utdanning_nus) = 1
                THEN MAX(utdanning_nus)
            ELSE 'flere_verdier'
        END AS utdanning_nus,
        CASE
            WHEN COUNT(DISTINCT utdanning_bestaatt) = 0
                THEN 'ikke_oppgitt'
            WHEN COUNT(DISTINCT utdanning_bestaatt) = 1
                THEN MAX(utdanning_bestaatt)
            ELSE 'flere_verdier'
        END AS utdanning_bestaatt,
        CASE
            WHEN COUNT(DISTINCT utdanning_godkjent) = 0
                THEN 'ikke_oppgitt'
            WHEN COUNT(DISTINCT utdanning_godkjent) = 1
                THEN MAX(utdanning_godkjent)
            ELSE 'flere_verdier'
        END AS utdanning_godkjent,
        CASE
            WHEN COUNTIF(helse_hindrer = 'ja') > 0 THEN 'ja'
            WHEN COUNTIF(helse_hindrer = 'vet_ikke') > 0 THEN 'vet_ikke'
            WHEN COUNTIF(helse_hindrer = 'nei') > 0 THEN 'nei'
            ELSE 'ikke_oppgitt'
        END AS helse_hindrer,
        CASE
            WHEN COUNTIF(andre_forhold_hindrer = 'ja') > 0 THEN 'ja'
            WHEN COUNTIF(andre_forhold_hindrer = 'vet_ikke') > 0
                THEN 'vet_ikke'
            WHEN COUNTIF(andre_forhold_hindrer = 'nei') > 0 THEN 'nei'
            ELSE 'ikke_oppgitt'
        END AS andre_forhold_hindrer,
        CASE
            WHEN COUNT(DISTINCT profilert_til) = 0
                THEN 'mangler_profilering'
            WHEN COUNT(DISTINCT profilert_til) = 1
                THEN MAX(profilert_til)
            ELSE 'flere_profileringsresultater'
        END AS profileringsresultat,
        CASE
            WHEN COUNT(DISTINCT aldersgruppe) = 0
                THEN 'mangler_profilering'
            WHEN COUNT(DISTINCT aldersgruppe) = 1
                THEN MAX(aldersgruppe)
            ELSE 'flere_verdier'
        END AS aldersgruppe,
        CASE
            WHEN COUNT(
                DISTINCT jobbet_sammenhengende_seks_av_tolv_siste_mnd
            ) = 0 THEN 'mangler_profilering'
            WHEN COUNT(
                DISTINCT jobbet_sammenhengende_seks_av_tolv_siste_mnd
            ) = 1 THEN IF(
                LOGICAL_OR(
                    jobbet_sammenhengende_seks_av_tolv_siste_mnd
                ),
                'ja',
                'nei'
            )
            ELSE 'flere_verdier'
        END AS jobbet_seks_av_tolv
    FROM sekvensgrunnlag
    GROUP BY person_id, sekvensnummer
),
dimensjoner AS (
    SELECT
        a.startdato,
        a.sluttdato,
        j.initial_status_set,
        a.profileringsresultat,
        a.aldersgruppe,
        a.utdanning_nus,
        a.utdanning_bestaatt,
        a.utdanning_godkjent,
        a.helse_hindrer,
        a.andre_forhold_hindrer,
        CASE
            WHEN a.helse_hindrer = 'ja'
                AND a.andre_forhold_hindrer = 'ja' THEN 'begge'
            WHEN a.helse_hindrer = 'ja' THEN 'bare_helse'
            WHEN a.andre_forhold_hindrer = 'ja' THEN 'bare_andre_forhold'
            WHEN a.helse_hindrer = 'nei'
                AND a.andre_forhold_hindrer = 'nei' THEN 'ingen'
            ELSE 'ukjent_eller_ikke_oppgitt'
        END AS hindringskombinasjon,
        a.jobbet_seks_av_tolv,
        j.styrk08_nivaa_2
    FROM attributter_per_sekvens a
    INNER JOIN initial_jobbsituasjon j
        USING (person_id, sekvensnummer)
),
aggregert AS (
    SELECT
        CASE startintervall
            WHEN 'maaned' THEN FORMAT_DATE('%Y-%m', startdato)
            WHEN 'kvartal' THEN FORMAT_DATE('%Y-Q%Q', startdato)
            WHEN 'halvaar' THEN FORMAT(
                '%d-H%d',
                EXTRACT(YEAR FROM startdato),
                IF(EXTRACT(MONTH FROM startdato) <= 6, 1, 2)
            )
            WHEN 'aar' THEN FORMAT_DATE('%Y', startdato)
            ELSE ERROR(
                FORMAT(
                    'Ugyldig startintervall: %s. '
                    || 'Bruk maaned, kvartal, halvaar eller aar.',
                    startintervall
                )
            )
        END AS startperiode,
        initial_status_set,
        profileringsresultat,
        aldersgruppe,
        utdanning_nus,
        utdanning_bestaatt,
        utdanning_godkjent,
        helse_hindrer,
        andre_forhold_hindrer,
        hindringskombinasjon,
        jobbet_seks_av_tolv,
        styrk08_nivaa_2,
        COUNT(*) AS antall_sekvenser,
        COUNTIF(
            sluttdato IS NOT NULL
            OR startdato <= DATE_SUB(analysedato, INTERVAL 30 DAY)
        ) AS observerbar_30,
        COUNTIF(
            sluttdato IS NOT NULL
            AND DATE_DIFF(sluttdato, startdato, DAY) < 30
        ) AS avsluttet_30,
        COUNTIF(
            sluttdato IS NOT NULL
            OR startdato <= DATE_SUB(analysedato, INTERVAL 90 DAY)
        ) AS observerbar_90,
        COUNTIF(
            sluttdato IS NOT NULL
            AND DATE_DIFF(sluttdato, startdato, DAY) < 90
        ) AS avsluttet_90,
        COUNTIF(
            sluttdato IS NOT NULL
            OR startdato <= DATE_SUB(analysedato, INTERVAL 180 DAY)
        ) AS observerbar_180,
        COUNTIF(
            sluttdato IS NOT NULL
            AND DATE_DIFF(sluttdato, startdato, DAY) < 180
        ) AS avsluttet_180,
        COUNTIF(
            sluttdato IS NOT NULL
            OR startdato <= DATE_SUB(analysedato, INTERVAL 365 DAY)
        ) AS observerbar_365,
        COUNTIF(
            sluttdato IS NOT NULL
            AND DATE_DIFF(sluttdato, startdato, DAY) < 365
        ) AS avsluttet_365,
        COUNTIF(sluttdato IS NULL) AS fortsatt_aktiv,
        COUNTIF(
            sluttdato IS NOT NULL
            AND DATE_DIFF(sluttdato, startdato, DAY) >= 365
        ) AS avsluttet_etter_365
    FROM dimensjoner
    GROUP BY
        startperiode,
        initial_status_set,
        profileringsresultat,
        aldersgruppe,
        utdanning_nus,
        utdanning_bestaatt,
        utdanning_godkjent,
        helse_hindrer,
        andre_forhold_hindrer,
        hindringskombinasjon,
        jobbet_seks_av_tolv,
        styrk08_nivaa_2
    HAVING COUNT(*) >= minstegruppe
)
SELECT *
FROM aggregert
ORDER BY
    startperiode,
    profileringsresultat,
    initial_status_set,
    aldersgruppe,
    utdanning_nus,
    utdanning_bestaatt,
    utdanning_godkjent,
    helse_hindrer,
    andre_forhold_hindrer,
    jobbet_seks_av_tolv,
    styrk08_nivaa_2;
