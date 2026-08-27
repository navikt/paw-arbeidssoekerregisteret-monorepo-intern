WITH DailyConfirmations AS (
    SELECT
        tidspunkt AS day,
        loesning,
        COUNT(*) AS confirmations
    FROM `arbeidssoekerregisteret_internt.bekreftelser`
    WHERE tidspunkt >= DATE '2025-09-01'
    GROUP BY tidspunkt, loesning
),

Solutions AS (
    SELECT loesning
    FROM `arbeidssoekerregisteret_internt.bekreftelser`
    WHERE tidspunkt >= DATE '2025-09-01'
    GROUP BY loesning
),

DateSeries AS (
    SELECT
        day,
        loesning
    FROM UNNEST(GENERATE_DATE_ARRAY('2025-09-29', '2100-12-31')) AS day
    CROSS JOIN Solutions
)

SELECT
    dates.day AS dag,
    dates.loesning,
    COALESCE(current_day.confirmations, 0) AS antall_leverte_bekreftelser,
    COALESCE(one_week_ago.confirmations, 0) AS antall_1_uke_siden,
    COALESCE(two_weeks_ago.confirmations, 0) AS antall_2_uker_siden,
    COALESCE(three_weeks_ago.confirmations, 0) AS antall_3_uker_siden,
    COALESCE(four_weeks_ago.confirmations, 0) AS antall_4_uker_siden
FROM DateSeries dates
LEFT JOIN DailyConfirmations current_day
    ON current_day.day = dates.day
    AND current_day.loesning = dates.loesning
LEFT JOIN DailyConfirmations one_week_ago
    ON one_week_ago.day = DATE_SUB(dates.day, INTERVAL 7 DAY)
    AND one_week_ago.loesning = dates.loesning
LEFT JOIN DailyConfirmations two_weeks_ago
    ON two_weeks_ago.day = DATE_SUB(dates.day, INTERVAL 14 DAY)
    AND two_weeks_ago.loesning = dates.loesning
LEFT JOIN DailyConfirmations three_weeks_ago
    ON three_weeks_ago.day = DATE_SUB(dates.day, INTERVAL 21 DAY)
    AND three_weeks_ago.loesning = dates.loesning
LEFT JOIN DailyConfirmations four_weeks_ago
    ON four_weeks_ago.day = DATE_SUB(dates.day, INTERVAL 28 DAY)
    AND four_weeks_ago.loesning = dates.loesning
