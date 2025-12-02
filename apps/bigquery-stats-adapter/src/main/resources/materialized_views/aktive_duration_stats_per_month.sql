WITH
-- Generate first day of each month
MonthStarts AS (
    SELECT DATE_TRUNC(date, MONTH) AS month_start
    FROM UNNEST(GENERATE_DATE_ARRAY('2020-01-01', '2027-01-01', INTERVAL 1 MONTH)) AS date
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

-- Calculate weeks since start for active periods at each month start
ActiveDurationsAtMonthStart AS (
    SELECT
        ms.month_start,
        DATE_DIFF(ms.month_start, ls.first_start, WEEK) AS weeks_since_start
    FROM MonthStarts ms
    LEFT JOIN LatestStatus ls
    ON
        -- Started on or before this month start
        ls.first_start <= ms.month_start
        AND
        -- Either hasn't ended yet OR ended after this month start
        (ls.last_end IS NULL OR ls.last_end > ms.month_start)
    WHERE ls.correlation_id IS NOT NULL
)

-- Calculate min, max, and median duration at each month start
SELECT
    month_start,
    MAX(weeks_since_start) AS max_weeks_active,
    APPROX_QUANTILES(weeks_since_start, 100)[OFFSET(50)] AS median_weeks_active,
    COUNT(*) AS total_active_count,
    COUNTIF(weeks_since_start < 4) AS count_0_to_4_weeks,
    COUNTIF(weeks_since_start >= 4 AND weeks_since_start < 8) AS count_4_to_8_weeks,
    COUNTIF(weeks_since_start >= 8 AND weeks_since_start < 16) AS count_8_to_16_weeks,
    COUNTIF(weeks_since_start >= 16 AND weeks_since_start < 26) AS count_16_to_26_weeks,
    COUNTIF(weeks_since_start >= 26 AND weeks_since_start < 52) AS count_26_to_52_weeks,
    COUNTIF(weeks_since_start >= 52 AND weeks_since_start < 104) AS count_52_to_104_weeks,
    COUNTIF(weeks_since_start >= 104) AS count_104_plus_weeks
FROM ActiveDurationsAtMonthStart
GROUP BY month_start
