WITH
-- Define a fixed start and end date
DateSeries AS (
    SELECT date AS day
FROM UNNEST(GENERATE_DATE_ARRAY('2020-01-01', '2026-01-01')) AS date
    ),

-- Get the latest status for each correlation_id
    LatestStatus AS (
SELECT
    correlation_id,
    MIN(startet.tidspunkt) AS first_start,
    MAX(CASE WHEN avsluttet IS NOT NULL THEN avsluttet.tidspunkt ELSE NULL END) AS last_end
FROM `arbeidssoekerregisteret_internt.perioder`
GROUP BY correlation_id
    ),

-- Count active correlation_ids for each day
    ActiveCountsByDay AS (
SELECT
    d.day,
    COUNT(DISTINCT ls.correlation_id) AS active_count
FROM DateSeries d
    LEFT JOIN LatestStatus ls
ON
    -- Started on or before this day
    ls.first_start <= d.day
    AND
    -- Either hasn't ended yet OR ended on or after this day
    (ls.last_end IS NULL OR ls.last_end > d.day)
GROUP BY d.day
    )

-- Get the final results with refresh timestamp
SELECT
    day,
    active_count
FROM ActiveCountsByDay