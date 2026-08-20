-- Manuell analyse av initialt permitterte og profileringsresultat.
-- Hele filen kan kjoeres som ett BigQuery-script.
-- Resultat 1: datadekning
-- Resultat 2: baseline per profileringsresultat
-- Resultat 3: kandidater til over- og underprofilering per gruppe

DECLARE kohort_fra DATE DEFAULT DATE '2025-01-01';
DECLARE grense_dager INT64 DEFAULT 180;

CREATE TEMP TABLE analysegrunnlag AS
WITH perioder_aggregert AS (
    SELECT
        correlation_id,
        MIN(startet.tidspunkt) AS startdato,
        MAX(avsluttet.tidspunkt) AS sluttdato
    FROM `arbeidssoekerregisteret_internt.perioder`
    GROUP BY correlation_id
),
modne_perioder AS (
    SELECT
        correlation_id,
        startdato,
        sluttdato
    FROM perioder_aggregert
    WHERE startdato >= kohort_fra
      AND startdato <= DATE_SUB(
          CURRENT_DATE('Europe/Oslo'),
          INTERVAL grense_dager DAY
      )
),
kandidater AS (
    SELECT
        p.correlation_id,
        p.startdato,
        p.sluttdato,
        o.opplysninger_id,
        o.helse.helsetilstand_hindrer_arbeid AS helse_hindrer,
        o.annet.andre_forhold_hindrer_arbeid AS andre_forhold_hindrer,
        o.jobbsituasjoner,
        pr.correlation_id AS profilering_correlation_id,
        pr.profilert_til,
        pr.jobbet_sammenhengende_seks_av_tolv_siste_mnd,
        pr.aldersgruppe
    FROM modne_perioder p
    INNER JOIN `arbeidssoekerregisteret_internt.opplysninger` o
        ON o.correlation_id = p.correlation_id
        AND o.sendt_inn_av.tidspunkt = p.startdato
    LEFT JOIN `arbeidssoekerregisteret_internt.profilering` pr
        ON pr.correlation_id = o.correlation_id
        AND pr.opplysninger_id = o.opplysninger_id
    WHERE EXISTS (
        SELECT 1
        FROM UNNEST(o.jobbsituasjoner) AS j
        WHERE j.beskrivelse = 'er_permittert'
    )
),
styrk_per_periode AS (
    SELECT
        c.correlation_id,
        ARRAY_AGG(
            DISTINCT CASE
                WHEN REGEXP_CONTAINS(j.stilling_styrk08, r'^[0-9]{4}$')
                    THEN SUBSTR(j.stilling_styrk08, 1, 2)
                ELSE 'ikke_oppgitt_eller_ugyldig'
            END
            ORDER BY CASE
                WHEN REGEXP_CONTAINS(j.stilling_styrk08, r'^[0-9]{4}$')
                    THEN SUBSTR(j.stilling_styrk08, 1, 2)
                ELSE 'ikke_oppgitt_eller_ugyldig'
            END
        ) AS styrk08_nivaa_2
    FROM kandidater c
    CROSS JOIN UNNEST(c.jobbsituasjoner) AS j
    WHERE j.beskrivelse = 'er_permittert'
    GROUP BY c.correlation_id
),
per_periode AS (
    SELECT
        c.correlation_id,
        MIN(c.startdato) AS startdato,
        MAX(c.sluttdato) AS sluttdato,
        COUNT(DISTINCT c.opplysninger_id) AS antall_initiale_opplysninger,
        COUNTIF(c.profilering_correlation_id IS NOT NULL) AS antall_profileringsrader,
        COUNT(DISTINCT c.profilert_til) AS antall_profileringsresultater,
        IF(
            COUNT(DISTINCT c.profilert_til) = 1,
            MAX(c.profilert_til),
            IF(
                COUNT(DISTINCT c.profilert_til) = 0,
                'mangler_profilering',
                'flere_profileringsresultater'
            )
        ) AS profilert_til,
        LOGICAL_OR(c.profilert_til = 'antatt_gode_muligheter') AS gode_muligheter,
        IF(
            COUNTIF(
                c.helse_hindrer = 'ja'
                OR c.andre_forhold_hindrer = 'ja'
            ) > 0,
            'ja',
            'nei'
        ) AS hindringer,
        CASE
            WHEN COUNTIF(c.helse_hindrer = 'ja') > 0 THEN 'ja'
            WHEN COUNTIF(c.helse_hindrer = 'vet_ikke') > 0 THEN 'vet_ikke'
            WHEN COUNTIF(c.helse_hindrer = 'nei') > 0 THEN 'nei'
            ELSE 'ikke_oppgitt'
        END AS helse_hindrer,
        CASE
            WHEN COUNTIF(c.andre_forhold_hindrer = 'ja') > 0 THEN 'ja'
            WHEN COUNTIF(c.andre_forhold_hindrer = 'vet_ikke') > 0 THEN 'vet_ikke'
            WHEN COUNTIF(c.andre_forhold_hindrer = 'nei') > 0 THEN 'nei'
            ELSE 'ikke_oppgitt'
        END AS andre_forhold_hindrer,
        CASE
            WHEN COUNT(DISTINCT c.aldersgruppe) = 0 THEN 'mangler_profilering'
            WHEN COUNT(DISTINCT c.aldersgruppe) = 1 THEN MAX(c.aldersgruppe)
            ELSE 'flere_verdier'
        END AS aldersgruppe,
        CASE
            WHEN COUNT(
                DISTINCT c.jobbet_sammenhengende_seks_av_tolv_siste_mnd
            ) = 0 THEN 'mangler_profilering'
            WHEN COUNT(
                DISTINCT c.jobbet_sammenhengende_seks_av_tolv_siste_mnd
            ) = 1 THEN IF(
                LOGICAL_OR(c.jobbet_sammenhengende_seks_av_tolv_siste_mnd),
                'ja',
                'nei'
            )
            ELSE 'flere_verdier'
        END AS jobbet_seks_av_tolv
    FROM kandidater c
    GROUP BY c.correlation_id
)
SELECT
    p.correlation_id,
    p.startdato,
    p.sluttdato,
    DATE_DIFF(p.sluttdato, p.startdato, DAY) AS varighet_dager,
    p.sluttdato IS NOT NULL
        AND DATE_DIFF(p.sluttdato, p.startdato, DAY) >= 0
        AND DATE_DIFF(p.sluttdato, p.startdato, DAY) < grense_dager
        AS klarte_seg_fint,
    p.sluttdato IS NULL OR p.sluttdato >= p.startdato AS gyldig_periode,
    p.antall_initiale_opplysninger,
    p.antall_profileringsrader,
    p.antall_profileringsresultater,
    p.antall_profileringsresultater = 1 AS entydig_profilering,
    p.profilert_til,
    IFNULL(p.gode_muligheter, FALSE) AS gode_muligheter,
    p.hindringer,
    p.helse_hindrer,
    p.andre_forhold_hindrer,
    p.aldersgruppe,
    p.jobbet_seks_av_tolv,
    s.styrk08_nivaa_2
FROM per_periode p
INNER JOIN styrk_per_periode s USING (correlation_id);


-- Resultat 1: Dekning og datakvalitet.
SELECT
    COUNT(*) AS initialt_permitterte_modne_perioder,
    COUNTIF(antall_profileringsrader > 0) AS med_profilering,
    COUNTIF(antall_profileringsrader = 0) AS uten_profilering,
    COUNTIF(entydig_profilering) AS med_entydig_profilering,
    COUNTIF(NOT entydig_profilering AND antall_profileringsrader > 0)
        AS med_flere_profileringsresultater,
    COUNTIF(antall_initiale_opplysninger > 1)
        AS med_flere_initiale_opplysninger,
    COUNTIF(NOT gyldig_periode) AS ugyldig_periodevarighet
FROM analysegrunnlag;


-- Resultat 2: Baseline per profileringsresultat.
SELECT
    profilert_til,
    COUNT(*) AS antall,
    COUNTIF(klarte_seg_fint) AS antall_under_180_dager,
    COUNTIF(NOT klarte_seg_fint) AS antall_minst_180_dager,
    ROUND(
        100 * SAFE_DIVIDE(COUNTIF(klarte_seg_fint), COUNT(*)),
        1
    ) AS prosent_under_180_dager,
    APPROX_QUANTILES(varighet_dager, 100 IGNORE NULLS)[OFFSET(50)]
        AS median_dager_blant_avsluttede
FROM analysegrunnlag
WHERE entydig_profilering
  AND gyldig_periode
GROUP BY profilert_til
ORDER BY antall DESC;


-- Resultat 3: Gruppene som kan vaere kandidater til feilprofilering.
WITH segmenter AS (
    SELECT
        correlation_id,
        gode_muligheter,
        klarte_seg_fint,
        'samlet' AS dimensjon,
        'alle' AS gruppe
    FROM analysegrunnlag
    WHERE entydig_profilering AND gyldig_periode

    UNION ALL

    SELECT
        correlation_id,
        gode_muligheter,
        klarte_seg_fint,
        'helse_hindrer' AS dimensjon,
        helse_hindrer AS gruppe
    FROM analysegrunnlag
    WHERE entydig_profilering AND gyldig_periode

    UNION ALL

    SELECT
        correlation_id,
        gode_muligheter,
        klarte_seg_fint,
        'andre_forhold_hindrer' AS dimensjon,
        andre_forhold_hindrer AS gruppe
    FROM analysegrunnlag
    WHERE entydig_profilering AND gyldig_periode

    UNION ALL

    SELECT
        correlation_id,
        gode_muligheter,
        klarte_seg_fint,
        'hindringer' AS dimensjon,
        hindringer AS gruppe
    FROM analysegrunnlag
    WHERE entydig_profilering AND gyldig_periode

    UNION ALL

    SELECT
        correlation_id,
        gode_muligheter,
        klarte_seg_fint,
        'aldersgruppe' AS dimensjon,
        aldersgruppe AS gruppe
    FROM analysegrunnlag
    WHERE entydig_profilering AND gyldig_periode

    UNION ALL

    SELECT
        correlation_id,
        gode_muligheter,
        klarte_seg_fint,
        'jobbet_seks_av_tolv' AS dimensjon,
        jobbet_seks_av_tolv AS gruppe
    FROM analysegrunnlag
    WHERE entydig_profilering AND gyldig_periode

    UNION ALL

    SELECT
        a.correlation_id,
        a.gode_muligheter,
        a.klarte_seg_fint,
        'styrk08_nivaa_2' AS dimensjon,
        styrk AS gruppe
    FROM analysegrunnlag a
    CROSS JOIN UNNEST(a.styrk08_nivaa_2) AS styrk
    WHERE a.entydig_profilering AND a.gyldig_periode
)
SELECT
    dimensjon,
    gruppe,
    COUNT(*) AS antall,
    COUNTIF(gode_muligheter) AS antall_profilert_gode_muligheter,
    COUNTIF(NOT gode_muligheter) AS antall_ikke_gode_muligheter,
    COUNTIF(gode_muligheter AND klarte_seg_fint)
        AS gode_muligheter_under_180,
    COUNTIF(gode_muligheter AND NOT klarte_seg_fint)
        AS kandidat_burde_kanskje_ikke,
    COUNTIF(NOT gode_muligheter AND klarte_seg_fint)
        AS kandidat_burde_kanskje,
    ROUND(
        100 * SAFE_DIVIDE(
            COUNTIF(gode_muligheter AND klarte_seg_fint),
            COUNTIF(gode_muligheter)
        ),
        1
    ) AS prosent_under_180_blant_gode_muligheter,
    ROUND(
        100 * SAFE_DIVIDE(
            COUNTIF(NOT gode_muligheter AND klarte_seg_fint),
            COUNTIF(NOT gode_muligheter)
        ),
        1
    ) AS prosent_under_180_blant_ikke_gode_muligheter
FROM segmenter
GROUP BY dimensjon, gruppe
HAVING dimensjon = 'samlet' OR COUNT(*) >= 30
ORDER BY dimensjon, antall DESC;
