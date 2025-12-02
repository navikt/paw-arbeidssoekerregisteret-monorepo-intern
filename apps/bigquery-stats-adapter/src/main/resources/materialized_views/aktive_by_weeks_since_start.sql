WITH
-- Define a fixed start and end date
DateSeries AS (
    SELECT date AS day
    FROM UNNEST(GENERATE_DATE_ARRAY('2020-01-01', '2027-01-01')) AS date
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

-- Calculate active periods with weeks since start
ActivePeriodsWithDuration AS (
    SELECT
        d.day,
        ls.correlation_id,
        DATE_DIFF(d.day, ls.first_start, WEEK) AS weeks_since_start
    FROM DateSeries d
    LEFT JOIN LatestStatus ls
    ON
        -- Started on or before this day
        ls.first_start <= d.day
        AND
        -- Either hasn't ended yet OR ended on or after this day
        (ls.last_end IS NULL OR ls.last_end > d.day)
    WHERE ls.correlation_id IS NOT NULL
)

-- Count active periods by day and weeks since start
SELECT
    day,
    weeks_since_start,
    COUNT(DISTINCT correlation_id) AS active_count
FROM ActivePeriodsWithDuration
GROUP BY day, weeks_since_start
