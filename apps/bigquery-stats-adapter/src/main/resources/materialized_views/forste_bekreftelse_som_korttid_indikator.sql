WITH
LatestPerPeriode AS (
    SELECT
        correlation_id,
        startet.tidspunkt AS startet_tidspunkt,
        avsluttet.tidspunkt AS avsluttet_tidspunkt
    FROM (
        SELECT
            *,
            ROW_NUMBER() OVER (
                PARTITION BY correlation_id
                ORDER BY
                    CASE WHEN avsluttet IS NOT NULL THEN 0 ELSE 1 END,
                    startet.tidspunkt DESC,
                    avsluttet.tidspunkt DESC NULLS LAST
            ) AS row_num
        FROM `arbeidssoekerregisteret_internt.perioder`
    )
    WHERE row_num = 1
      AND avsluttet IS NOT NULL
),
-- First bekreftelse per closed period (chronologically earliest)
ForsteBekreftelse AS (
    SELECT
        b.correlation_id,
        b.har_jobbet AS forste_har_jobbet
    FROM (
        SELECT
            b.*,
            ROW_NUMBER() OVER (
                PARTITION BY b.correlation_id
                ORDER BY b.tidspunkt ASC, b.gjelder_fra ASC
            ) AS row_num
        FROM `arbeidssoekerregisteret_internt.bekreftelser` b
        INNER JOIN LatestPerPeriode lp ON b.correlation_id = lp.correlation_id
    )
    WHERE row_num = 1
),
PeriodsWithDuration AS (
    SELECT
        FORMAT_DATE('%Y-%m', lp.startet_tidspunkt) AS start_maaned,
        CASE
            WHEN fb.correlation_id IS NULL THEN 'ingen_bekreftelse'
            WHEN fb.forste_har_jobbet THEN 'har_jobbet'
            ELSE 'har_ikke_jobbet'
        END AS forste_bekreftelse_status,
        DATE_DIFF(lp.avsluttet_tidspunkt, lp.startet_tidspunkt, DAY) AS varighet_dager
    FROM LatestPerPeriode lp
    LEFT JOIN ForsteBekreftelse fb ON lp.correlation_id = fb.correlation_id
    WHERE DATE_DIFF(lp.avsluttet_tidspunkt, lp.startet_tidspunkt, DAY) >= 0
)
SELECT
    start_maaned,
    forste_bekreftelse_status,
    CASE
        WHEN varighet_dager = 0 THEN '0 dager'
        WHEN varighet_dager <= 3 THEN '1-3 dager'
        WHEN varighet_dager <= 7 THEN '4-7 dager'
        WHEN varighet_dager <= 14 THEN '8-14 dager'
        WHEN varighet_dager <= 30 THEN '15-30 dager'
        WHEN varighet_dager <= 90 THEN '31-90 dager'
        WHEN varighet_dager <= 182 THEN '91-182 dager'
        WHEN varighet_dager <= 365 THEN '183-365 dager'
        ELSE '365+ dager'
    END AS varighet_bucket,
    COUNT(*) AS antall
FROM PeriodsWithDuration
GROUP BY start_maaned, forste_bekreftelse_status, varighet_bucket
