WITH
-- Find first avvist events that match our criteria (replacing QUALIFY with GROUP BY)
FirstAvvist AS (
    SELECT
        id,
        MIN(metadata.tidspunkt) AS avvist_time
    FROM `arbeidssoekerregisteret_internt.hendelser`
    WHERE
        type = 'intern.v1.avvist'
      AND 'er_under_18_aar' IN UNNEST(options)
    AND 'bosatt_etter_freg_loven' IN UNNEST(options)
    AND metadata.brukertype = 'sluttbruker'
GROUP BY id
    ),

-- Find first startet events (replacing QUALIFY with GROUP BY)
    FirstStartet AS (
SELECT
    id,
    'er_under_18_aar' IN UNNEST(options) as er_under_18_aar,
    MIN(metadata.tidspunkt) AS startet_time
FROM `arbeidssoekerregisteret_internt.hendelser`
WHERE type = 'intern.v1.startet'
GROUP BY id, 'er_under_18_aar' IN UNNEST(options)
    ),

-- Calculate latency between events, including IDs with no startet event
    LatencyData AS (
SELECT
    a.id,
    DATE_DIFF(s.startet_time, a.avvist_time, DAY) AS latency_days,
    s.er_under_18_aar,
    CASE
    WHEN s.id IS NULL THEN 'Aldri startet'
    WHEN DATE_DIFF(s.startet_time, a.avvist_time, DAY) BETWEEN 0 AND 7 THEN '0-7 dager'
    WHEN DATE_DIFF(s.startet_time, a.avvist_time, DAY) BETWEEN 8 AND 14 THEN '8-14 dager'
    WHEN DATE_DIFF(s.startet_time, a.avvist_time, DAY) BETWEEN 15 AND 30 THEN '15-30 dager'
    WHEN DATE_DIFF(s.startet_time, a.avvist_time, DAY) BETWEEN 31 AND 60 THEN '31-60 dager'
    WHEN DATE_DIFF(s.startet_time, a.avvist_time, DAY) BETWEEN 61 AND 90 THEN '61-90 dager'
    WHEN DATE_DIFF(s.startet_time, a.avvist_time, DAY) BETWEEN 91 AND 180 THEN '91-180 dager'
    WHEN DATE_DIFF(s.startet_time, a.avvist_time, DAY) BETWEEN 181 AND 365 THEN '181-365 dager'
    ELSE 'Over 365 dager'
    END AS latency_bucket
FROM FirstAvvist a
    LEFT JOIN FirstStartet s ON a.id = s.id
-- Only keep cases where startet_time is after avvist_time or startet_time is NULL
WHERE s.startet_time >= a.avvist_time OR s.startet_time IS NULL
    )

-- Group by bucket and summarize
SELECT
    er_under_18_aar AS under_18_ved_start,
    latency_bucket,
    COUNT(*) AS count,
    MIN(latency_days) AS min_days,
    MAX(latency_days) AS max_days,
    AVG(latency_days) AS avg_days
FROM LatencyData
GROUP BY er_under_18_aar, latency_bucket
