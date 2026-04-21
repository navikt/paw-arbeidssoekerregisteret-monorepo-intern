WITH LatestPerPeriode AS (
    SELECT
        correlation_id,
        startet.tidspunkt AS startet_tidspunkt,
        avsluttet.tidspunkt AS avsluttet_tidspunkt,
        avsluttet.aarsak AS avsluttet_aarsak
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
PeriodsWithDuration AS (
    SELECT
        FORMAT_DATE('%Y-%m', avsluttet_tidspunkt) AS avsluttet_maaned,
        avsluttet_aarsak,
        DATE_DIFF(avsluttet_tidspunkt, startet_tidspunkt, DAY) AS varighet_dager
    FROM LatestPerPeriode
)
SELECT
    avsluttet_maaned,
    avsluttet_aarsak,
    CASE
        WHEN varighet_dager < 0 THEN 'ukjent_varighet'
        WHEN varighet_dager = 0 THEN '0 dager'
        WHEN varighet_dager <= 3 THEN '1-3 dager'
        WHEN varighet_dager <= 7 THEN '4-7 dager'
        WHEN varighet_dager <= 14 THEN '8-14 dager'
        WHEN varighet_dager <= 30 THEN '15-30 dager'
        WHEN varighet_dager <= 90 THEN '31-90 dager'
        WHEN varighet_dager <= 180 THEN '91-180 dager'
        WHEN varighet_dager <= 365 THEN '181-365 dager'
        ELSE '365+ dager'
    END AS varighet_bucket,
    COUNT(*) AS antall
FROM PeriodsWithDuration
GROUP BY avsluttet_maaned, avsluttet_aarsak, varighet_bucket
