WITH
-- Find first avvist events that match our criteria (without QUALIFY)
FirstAvvist AS (SELECT id,
                       MIN(metadata.tidspunkt) AS avvist_time
                FROM `arbeidssoekerregisteret_internt.hendelser`
                WHERE type = 'intern.v1.avvist'
                  AND 'er_under_18_aar' NOT IN UNNEST(options)
    AND metadata.brukertype = 'sluttbruker'
GROUP BY id),

-- Find first startet events (without QUALIFY)
    FirstStartet AS (
SELECT
    id, MIN (metadata.tidspunkt) AS startet_time
FROM `arbeidssoekerregisteret_internt.hendelser`
WHERE type = 'intern.v1.startet'
GROUP BY id),

-- Group avvist events by month and count total
    AvvistByMonth AS (
SELECT
    FORMAT_DATE('%Y-%m', avvist_time) AS month_bucket, COUNT (*) AS total_avvist_count
FROM FirstAvvist
GROUP BY month_bucket),

-- Calculate latency between events (only for IDs that have both events)
    LatencyData AS (
SELECT
    a.id, a.avvist_time, s.startet_time, DATE_DIFF(s.startet_time, a.avvist_time, DAY) AS latency_days, FORMAT_DATE('%Y-%m', a.avvist_time) AS month_bucket
FROM FirstAvvist a
    INNER JOIN FirstStartet s
ON a.id = s.id
WHERE s.startet_time >= a.avvist_time),

-- Pre-calculate percentiles by month
    PercentilesByMonth AS (
SELECT
    month_bucket, APPROX_QUANTILES(latency_days, 100)[
OFFSET(50)] AS median_latency_days, APPROX_QUANTILES(latency_days, 100)[OFFSET(90)] AS p90_latency_days
FROM LatencyData
GROUP BY month_bucket)

-- Group by month to show trend over time
SELECT a.month_bucket,
       a.total_avvist_count                                                          AS total_events,
       COUNT(l.id)                                                                   AS events_with_startet,
       (a.total_avvist_count - COUNT(l.id))                                          AS missing_startet_events,
       SAFE_DIVIDE((a.total_avvist_count - COUNT(l.id)), a.total_avvist_count) * 100 AS missing_percentage,
       AVG(l.latency_days)                                                           AS avg_latency_days,
       p.median_latency_days                                                         AS median_latency_days,
       MIN(l.latency_days)                                                           AS min_latency_days,
       MAX(l.latency_days)                                                           AS max_latency_days,
       p.p90_latency_days                                                            AS p90_latency_days
FROM AvvistByMonth a
         LEFT JOIN LatencyData l ON a.month_bucket = l.month_bucket
         LEFT JOIN PercentilesByMonth p ON a.month_bucket = p.month_bucket
GROUP BY a.month_bucket,
         a.total_avvist_count,
         p.median_latency_days,
         p.p90_latency_days
