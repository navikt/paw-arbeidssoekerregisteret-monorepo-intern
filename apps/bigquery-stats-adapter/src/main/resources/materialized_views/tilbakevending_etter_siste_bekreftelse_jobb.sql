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
),
-- Last bekreftelse per closed period, constrained to confirmations received before the close date
SisteBekreftelse AS (
    SELECT
        correlation_id,
        har_jobbet AS siste_har_jobbet
    FROM (
        SELECT
            b.*,
            ROW_NUMBER() OVER (
                PARTITION BY b.correlation_id
                ORDER BY b.tidspunkt DESC, b.gjelder_til DESC
            ) AS row_num
        FROM `arbeidssoekerregisteret_internt.bekreftelser` b
        INNER JOIN LatestPerPeriode lp
            ON b.correlation_id = lp.correlation_id
            AND lp.avsluttet_tidspunkt IS NOT NULL
            AND b.tidspunkt <= lp.avsluttet_tidspunkt
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
-- All periods (open and closed) with person ID, ordered per person for LEAD
AllePerioderMedPerson AS (
    SELECT
        lp.correlation_id,
        pp.person_id,
        lp.startet_tidspunkt,
        lp.avsluttet_tidspunkt
    FROM LatestPerPeriode lp
    INNER JOIN PeriodeWithPerson pp ON lp.correlation_id = pp.correlation_id
),
-- Use LEAD to find each period's next start within the same person's history
MedNestePeriode AS (
    SELECT
        correlation_id,
        person_id,
        avsluttet_tidspunkt,
        LEAD(startet_tidspunkt) OVER (
            PARTITION BY person_id
            ORDER BY startet_tidspunkt, correlation_id
        ) AS neste_start
    FROM AllePerioderMedPerson
),
AvsluttedeMedNestePeriode AS (
    SELECT
        mn.correlation_id,
        mn.avsluttet_tidspunkt,
        mn.neste_start,
        sb.siste_har_jobbet
    FROM MedNestePeriode mn
    LEFT JOIN SisteBekreftelse sb ON mn.correlation_id = sb.correlation_id
    WHERE mn.avsluttet_tidspunkt IS NOT NULL
)
SELECT
    FORMAT_DATE('%Y-%m', avsluttet_tidspunkt) AS avsluttet_maaned,
    CASE
        WHEN siste_har_jobbet IS NULL THEN 'ingen_bekreftelse'
        WHEN siste_har_jobbet THEN 'hadde_jobb'
        ELSE 'hadde_ikke_jobb'
    END AS siste_bekreftelse_jobb_status,
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
FROM AvsluttedeMedNestePeriode
GROUP BY avsluttet_maaned, siste_bekreftelse_jobb_status, tid_til_retur_bucket
