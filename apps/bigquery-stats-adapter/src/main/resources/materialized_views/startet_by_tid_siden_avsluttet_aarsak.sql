WITH
-- Get active periods with person ID
ActivePeriodsWithPerson AS (
    SELECT
        p.correlation_id,
        p.startet.tidspunkt AS startet_tidspunkt,
        p.startet.brukertype AS startet_brukertype,
        h.id AS person_id
    FROM `arbeidssoekerregisteret_internt.perioder` p
    INNER JOIN `arbeidssoekerregisteret_internt.hendelser` h
        ON p.correlation_id = h.correlation_id
    WHERE p.avsluttet IS NULL
),

-- Get all ended periods by person
EndedPeriodsByPerson AS (
    SELECT
        h.id AS person_id,
        p.avsluttet.tidspunkt AS avsluttet_tidspunkt,
        p.avsluttet.aarsak AS avsluttet_aarsak
    FROM `arbeidssoekerregisteret_internt.perioder` p
    INNER JOIN `arbeidssoekerregisteret_internt.hendelser` h
        ON p.correlation_id = h.correlation_id
    WHERE p.avsluttet IS NOT NULL
),

-- Find the most recent ended period before each active period start
PreviousEndByPerson AS (
    SELECT
        ap.correlation_id,
        ap.startet_tidspunkt,
        ap.startet_brukertype,
        ap.person_id,
        MAX(ep.avsluttet_tidspunkt) AS previous_avsluttet,
        MAX_BY(ep.avsluttet_aarsak, ep.avsluttet_tidspunkt) AS previous_avsluttet_aarsak
    FROM ActivePeriodsWithPerson ap
    LEFT JOIN EndedPeriodsByPerson ep
        ON ap.person_id = ep.person_id
        AND ep.avsluttet_tidspunkt < ap.startet_tidspunkt
    GROUP BY ap.correlation_id, ap.startet_tidspunkt, ap.startet_brukertype, ap.person_id
),

-- Calculate days since last ended period and assign to buckets
PeriodsWithTimeSinceEnd AS (
    SELECT
        correlation_id,
        startet_tidspunkt,
        startet_brukertype,
        person_id,
        previous_avsluttet,
        previous_avsluttet_aarsak,
        CASE
            WHEN previous_avsluttet IS NULL THEN 'first'
            WHEN DATE_DIFF(startet_tidspunkt, previous_avsluttet, DAY) <= 1 THEN '0-1 days'
            WHEN DATE_DIFF(startet_tidspunkt, previous_avsluttet, DAY) <= 4 THEN '1-4 days'
            WHEN DATE_DIFF(startet_tidspunkt, previous_avsluttet, DAY) <= 8 THEN '4-8 days'
            WHEN DATE_DIFF(startet_tidspunkt, previous_avsluttet, DAY) <= 16 THEN '8-16 days'
            WHEN DATE_DIFF(startet_tidspunkt, previous_avsluttet, DAY) <= 30 THEN '16-30 days'
            WHEN DATE_DIFF(startet_tidspunkt, previous_avsluttet, DAY) <= 180 THEN '30-180 days'
            WHEN DATE_DIFF(startet_tidspunkt, previous_avsluttet, DAY) <= 365 THEN '180-365 days'
            ELSE '365+ days'
        END AS time_since_last_avsluttet_bucket
    FROM PreviousEndByPerson
)

-- Count active periods by time bucket
SELECT
    startet_tidspunkt,
    startet_brukertype,
    previous_avsluttet_aarsak,
    time_since_last_avsluttet_bucket,
    COUNT(*) AS antall_aktive_perioder
FROM PeriodsWithTimeSinceEnd
GROUP BY startet_tidspunkt, startet_brukertype, previous_avsluttet_aarsak, time_since_last_avsluttet_bucket
