WITH DailyStarts AS (
    SELECT
        startet.tidspunkt AS day,
        COUNT(DISTINCT correlation_id) AS period_starts
    FROM `arbeidssoekerregisteret_internt.perioder`
    GROUP BY startet.tidspunkt
),

DateSeries AS (
    SELECT day
    FROM UNNEST(GENERATE_DATE_ARRAY('2020-01-29', '2100-12-31')) AS day
)

SELECT
    dates.day AS dag,
    COALESCE(current_day.period_starts, 0) AS antall_periodestarter,
    COALESCE(one_week_ago.period_starts, 0) AS antall_1_uke_siden,
    COALESCE(two_weeks_ago.period_starts, 0) AS antall_2_uker_siden,
    COALESCE(three_weeks_ago.period_starts, 0) AS antall_3_uker_siden,
    COALESCE(four_weeks_ago.period_starts, 0) AS antall_4_uker_siden
FROM DateSeries dates
LEFT JOIN DailyStarts current_day
    ON current_day.day = dates.day
LEFT JOIN DailyStarts one_week_ago
    ON one_week_ago.day = DATE_SUB(dates.day, INTERVAL 7 DAY)
LEFT JOIN DailyStarts two_weeks_ago
    ON two_weeks_ago.day = DATE_SUB(dates.day, INTERVAL 14 DAY)
LEFT JOIN DailyStarts three_weeks_ago
    ON three_weeks_ago.day = DATE_SUB(dates.day, INTERVAL 21 DAY)
LEFT JOIN DailyStarts four_weeks_ago
    ON four_weeks_ago.day = DATE_SUB(dates.day, INTERVAL 28 DAY)
