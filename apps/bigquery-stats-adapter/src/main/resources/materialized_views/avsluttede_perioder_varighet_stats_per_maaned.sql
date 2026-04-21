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
    WHERE DATE_DIFF(avsluttet_tidspunkt, startet_tidspunkt, DAY) >= 0
)
SELECT
    avsluttet_maaned,
    avsluttet_aarsak,
    COUNT(*) AS antall_avsluttet,
    AVG(varighet_dager) AS gjennomsnitt_dager,
    APPROX_QUANTILES(varighet_dager, 100)[OFFSET(25)] AS p25_dager,
    APPROX_QUANTILES(varighet_dager, 100)[OFFSET(50)] AS median_dager,
    APPROX_QUANTILES(varighet_dager, 100)[OFFSET(75)] AS p75_dager,
    APPROX_QUANTILES(varighet_dager, 100)[OFFSET(90)] AS p90_dager,
    COUNTIF(varighet_dager <= 14) AS antall_korte_perioder,
    COUNTIF(varighet_dager > 14 AND varighet_dager <= 182) AS antall_medium_perioder,
    COUNTIF(varighet_dager > 182) AS antall_lange_perioder
FROM PeriodsWithDuration
GROUP BY avsluttet_maaned, avsluttet_aarsak
