WITH
-- Find first avvist events that match our criteria
    FirstAvvist AS (
        SELECT
            id,
            metadata.tidspunkt AS avvist_time
        FROM `arbeidssoekerregisteret_internt.hendelser`
        WHERE
            type = 'intern.v1.avvist'
          AND 'er_under_18_aar' IN UNNEST(options)
    AND 'bosatt_etter_freg_loven' IN UNNEST(options)
    AND metadata.brukertype = 'sluttbruker'
    QUALIFY ROW_NUMBER() OVER (PARTITION BY id ORDER BY metadata.tidspunkt ASC) = 1
    ),

-- Find first startet events
    FirstStartet AS (
SELECT
    id,
    metadata.tidspunkt AS startet_time
FROM `arbeidssoekerregisteret_internt.hendelser`
WHERE type = 'intern.v1.startet'
    QUALIFY ROW_NUMBER() OVER (PARTITION BY id ORDER BY metadata.tidspunkt ASC) = 1
    ),

-- Group avvist events by month and count total
    AvvistByMonth AS (
SELECT
    FORMAT_DATE('%Y-%m', avvist_time) AS month_bucket,
    COUNT(*) AS total_avvist_count
FROM FirstAvvist
GROUP BY month_bucket
    ),

-- Calculate latency between events (only for IDs that have both events)
    LatencyData AS (
SELECT
    a.id,
    a.avvist_time,
    s.startet_time,
    DATE_DIFF(s.startet_time, a.avvist_time, DAY) AS latency_days,
    FORMAT_DATE('%Y-%m', a.avvist_time) AS month_bucket
FROM FirstAvvist a
    LEFT JOIN FirstStartet s ON a.id = s.id
WHERE s.startet_time >= a.avvist_time
    )

-- Group by month to show trend over time
SELECT
    a.month_bucket,
    a.total_avvist_count AS avvist,
    COUNT(l.id) AS startet_etter_avvist,
    (a.total_avvist_count - COUNT(l.id)) AS avvist_ikke_startet,
    SAFE_DIVIDE((a.total_avvist_count - COUNT(l.id)), a.total_avvist_count) * 100 AS missing_percentage,
    AVG(l.latency_days) AS avg_latency_days,
    APPROX_QUANTILES(l.latency_days, 100)[OFFSET(50)] AS median_latency_days,
    MIN(l.latency_days) AS min_latency_days,
    MAX(l.latency_days) AS max_latency_days,
    APPROX_QUANTILES(l.latency_days, 100)[OFFSET(90)] AS p90_latency_days
FROM AvvistByMonth a
         LEFT JOIN LatencyData l ON a.month_bucket = l.month_bucket
GROUP BY a.month_bucket, a.total_avvist_count
ORDER BY a.month_bucket