WITH
LatestPerPeriode AS (
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
),
PeriodeWithPerson AS (
    SELECT
        correlation_id,
        id AS person_id
    FROM `arbeidssoekerregisteret_internt.hendelser`
    WHERE type = 'intern.v1.startet'
),
PerioderMedPersonId AS (
    SELECT
        lp.correlation_id,
        pp.person_id,
        lp.startet_tidspunkt,
        lp.avsluttet_tidspunkt,
        lp.avsluttet_aarsak
    FROM LatestPerPeriode lp
    INNER JOIN PeriodeWithPerson pp ON lp.correlation_id = pp.correlation_id
),
MedNestePeriode AS (
    SELECT
        correlation_id,
        avsluttet_tidspunkt,
        avsluttet_aarsak,
        LEAD(startet_tidspunkt) OVER (
            PARTITION BY person_id
            ORDER BY startet_tidspunkt, correlation_id
        ) AS neste_start
    FROM PerioderMedPersonId
)
SELECT
    FORMAT_DATE('%Y-%m', avsluttet_tidspunkt) AS avsluttet_maaned,
    avsluttet_aarsak,
    CASE
        WHEN neste_start IS NULL THEN 'ikke_returnert'
        WHEN DATE_DIFF(neste_start, avsluttet_tidspunkt, DAY) < 0 THEN 'ukjent_rekkefølge'
        WHEN DATE_DIFF(neste_start, avsluttet_tidspunkt, DAY) <= 7 THEN '0-7 dager'
        WHEN DATE_DIFF(neste_start, avsluttet_tidspunkt, DAY) <= 14 THEN '8-14 dager'
        WHEN DATE_DIFF(neste_start, avsluttet_tidspunkt, DAY) <= 30 THEN '15-30 dager'
        WHEN DATE_DIFF(neste_start, avsluttet_tidspunkt, DAY) <= 90 THEN '31-90 dager'
        WHEN DATE_DIFF(neste_start, avsluttet_tidspunkt, DAY) <= 180 THEN '91-180 dager'
        WHEN DATE_DIFF(neste_start, avsluttet_tidspunkt, DAY) <= 365 THEN '181-365 dager'
        ELSE '365+ dager'
    END AS tid_til_retur_bucket,
    COUNT(*) AS antall
FROM MedNestePeriode
WHERE avsluttet_tidspunkt IS NOT NULL
GROUP BY avsluttet_maaned, avsluttet_aarsak, tid_til_retur_bucket
