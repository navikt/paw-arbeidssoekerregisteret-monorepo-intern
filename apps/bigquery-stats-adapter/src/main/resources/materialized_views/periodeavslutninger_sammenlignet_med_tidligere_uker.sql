WITH DailyEnds AS (
    SELECT
        avsluttet.tidspunkt AS day,
        COUNT(DISTINCT correlation_id) AS period_ends
    FROM `arbeidssoekerregisteret_internt.perioder`
    WHERE avsluttet IS NOT NULL
    GROUP BY avsluttet.tidspunkt
),

DateSeries AS (
    SELECT day
    FROM UNNEST(GENERATE_DATE_ARRAY('2020-01-29', '2100-12-31')) AS day
)

SELECT
    dates.day AS dag,
    COALESCE(current_day.period_ends, 0) AS antall_periodeavslutninger,
    COALESCE(one_week_ago.period_ends, 0) AS antall_1_uke_siden,
    COALESCE(two_weeks_ago.period_ends, 0) AS antall_2_uker_siden,
    COALESCE(three_weeks_ago.period_ends, 0) AS antall_3_uker_siden,
    COALESCE(four_weeks_ago.period_ends, 0) AS antall_4_uker_siden
FROM DateSeries dates
LEFT JOIN DailyEnds current_day
    ON current_day.day = dates.day
LEFT JOIN DailyEnds one_week_ago
    ON one_week_ago.day = DATE_SUB(dates.day, INTERVAL 7 DAY)
LEFT JOIN DailyEnds two_weeks_ago
    ON two_weeks_ago.day = DATE_SUB(dates.day, INTERVAL 14 DAY)
LEFT JOIN DailyEnds three_weeks_ago
    ON three_weeks_ago.day = DATE_SUB(dates.day, INTERVAL 21 DAY)
LEFT JOIN DailyEnds four_weeks_ago
    ON four_weeks_ago.day = DATE_SUB(dates.day, INTERVAL 28 DAY)
