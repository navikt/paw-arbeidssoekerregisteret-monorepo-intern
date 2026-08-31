WITH ClosedPeriods AS (
    SELECT
        correlation_id,
        MAX(avsluttet.tidspunkt) AS last_end
    FROM `arbeidssoekerregisteret_internt.perioder`
    GROUP BY correlation_id
    HAVING last_end >= DATE '2025-09-01'
),

LatestConfirmationPerRange AS (
    SELECT
        correlation_id,
        tidspunkt,
        gjelder_fra,
        gjelder_til,
        har_jobbet
    FROM (
        SELECT
            correlation_id,
            tidspunkt,
            gjelder_fra,
            gjelder_til,
            har_jobbet,
            ROW_NUMBER() OVER (
                PARTITION BY correlation_id, gjelder_fra, gjelder_til
                ORDER BY tidspunkt DESC
            ) AS row_num
        FROM `arbeidssoekerregisteret_internt.bekreftelser`
    )
    WHERE row_num = 1
),

RankedConfirmations AS (
    SELECT
        periods.correlation_id,
        confirmations.har_jobbet,
        ROW_NUMBER() OVER (
            PARTITION BY periods.correlation_id
            ORDER BY
                confirmations.gjelder_til DESC,
                confirmations.gjelder_fra DESC,
                confirmations.tidspunkt DESC
        ) AS confirmation_number
    FROM ClosedPeriods periods
    INNER JOIN LatestConfirmationPerRange confirmations
        ON confirmations.correlation_id = periods.correlation_id
),

StreakPerPeriod AS (
    SELECT
        correlation_id,
        COALESCE(
            MIN(IF(NOT har_jobbet, confirmation_number, NULL)) - 1,
            COUNT(*)
        ) AS consecutive_worked
    FROM RankedConfirmations
    GROUP BY correlation_id
),

WeeklyCounts AS (
    SELECT
        DATE_TRUNC(periods.last_end, ISOWEEK) AS week_start,
        EXTRACT(ISOYEAR FROM periods.last_end) AS iso_year,
        EXTRACT(ISOWEEK FROM periods.last_end) AS iso_week,
        COUNT(*) AS closed_periods,
        COUNTIF(streak.consecutive_worked IS NULL) AS no_confirmations,
        COUNTIF(streak.consecutive_worked = 0) AS consecutive_worked_0,
        COUNTIF(streak.consecutive_worked = 1) AS consecutive_worked_1,
        COUNTIF(streak.consecutive_worked = 2) AS consecutive_worked_2,
        COUNTIF(streak.consecutive_worked = 3) AS consecutive_worked_3,
        COUNTIF(streak.consecutive_worked = 4) AS consecutive_worked_4,
        COUNTIF(streak.consecutive_worked = 5) AS consecutive_worked_5,
        COUNTIF(streak.consecutive_worked = 6) AS consecutive_worked_6,
        COUNTIF(streak.consecutive_worked = 7) AS consecutive_worked_7,
        COUNTIF(streak.consecutive_worked = 8) AS consecutive_worked_8,
        COUNTIF(streak.consecutive_worked = 9) AS consecutive_worked_9,
        COUNTIF(streak.consecutive_worked >= 10) AS consecutive_worked_10_plus
    FROM ClosedPeriods periods
    LEFT JOIN StreakPerPeriod streak
        ON streak.correlation_id = periods.correlation_id
    GROUP BY week_start, iso_year, iso_week
)

SELECT
    week_start AS uke_start,
    iso_year AS iso_aar,
    iso_week AS iso_uke,
    closed_periods AS antall_avsluttede_perioder,
    no_confirmations AS antall_ingen_bekreftelser,
    consecutive_worked_0 AS antall_sammenhengende_ja_0,
    consecutive_worked_1 AS antall_sammenhengende_ja_1,
    consecutive_worked_2 AS antall_sammenhengende_ja_2,
    consecutive_worked_3 AS antall_sammenhengende_ja_3,
    consecutive_worked_4 AS antall_sammenhengende_ja_4,
    consecutive_worked_5 AS antall_sammenhengende_ja_5,
    consecutive_worked_6 AS antall_sammenhengende_ja_6,
    consecutive_worked_7 AS antall_sammenhengende_ja_7,
    consecutive_worked_8 AS antall_sammenhengende_ja_8,
    consecutive_worked_9 AS antall_sammenhengende_ja_9,
    consecutive_worked_10_plus AS antall_sammenhengende_ja_10_pluss,
    SAFE_DIVIDE(no_confirmations, closed_periods) AS andel_ingen_bekreftelser,
    SAFE_DIVIDE(consecutive_worked_0, closed_periods) AS andel_sammenhengende_ja_0,
    SAFE_DIVIDE(consecutive_worked_1, closed_periods) AS andel_sammenhengende_ja_1,
    SAFE_DIVIDE(consecutive_worked_2, closed_periods) AS andel_sammenhengende_ja_2,
    SAFE_DIVIDE(consecutive_worked_3, closed_periods) AS andel_sammenhengende_ja_3,
    SAFE_DIVIDE(consecutive_worked_4, closed_periods) AS andel_sammenhengende_ja_4,
    SAFE_DIVIDE(consecutive_worked_5, closed_periods) AS andel_sammenhengende_ja_5,
    SAFE_DIVIDE(consecutive_worked_6, closed_periods) AS andel_sammenhengende_ja_6,
    SAFE_DIVIDE(consecutive_worked_7, closed_periods) AS andel_sammenhengende_ja_7,
    SAFE_DIVIDE(consecutive_worked_8, closed_periods) AS andel_sammenhengende_ja_8,
    SAFE_DIVIDE(consecutive_worked_9, closed_periods) AS andel_sammenhengende_ja_9,
    SAFE_DIVIDE(
        consecutive_worked_10_plus,
        closed_periods
    ) AS andel_sammenhengende_ja_10_pluss
FROM WeeklyCounts
