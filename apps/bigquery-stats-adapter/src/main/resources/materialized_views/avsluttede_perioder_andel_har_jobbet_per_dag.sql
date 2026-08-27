WITH DateBounds AS (
    SELECT
        GREATEST(
            COALESCE(MAX(avsluttet.tidspunkt), DATE '2025-09-01'),
            DATE '2025-09-01'
        ) AS last_day
    FROM `arbeidssoekerregisteret_internt.perioder`
    WHERE avsluttet IS NOT NULL
),

DateSeries AS (
    SELECT day
    FROM DateBounds
    CROSS JOIN UNNEST(
        GENERATE_DATE_ARRAY(DATE '2025-09-01', last_day)
    ) AS day
),

ClosedPeriods AS (
    SELECT
        correlation_id,
        MIN(startet.tidspunkt) AS first_start,
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
            *,
            ROW_NUMBER() OVER (
                PARTITION BY correlation_id, gjelder_fra, gjelder_til
                ORDER BY tidspunkt DESC
            ) AS row_num
        FROM `arbeidssoekerregisteret_internt.bekreftelser`
        WHERE tidspunkt >= DATE '2025-09-01'
    )
    WHERE row_num = 1
),

RankedConfirmations AS (
    SELECT
        periods.correlation_id,
        periods.last_end,
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

SharesPerPeriod AS (
    SELECT
        correlation_id,
        last_end,
        COUNT(*) AS confirmation_count,
        IF(
            COUNT(*) >= 2,
            SAFE_DIVIDE(COUNTIF(confirmation_number <= 2 AND har_jobbet), 2),
            NULL
        ) AS share_worked_last_2,
        IF(
            COUNT(*) >= 10,
            SAFE_DIVIDE(COUNTIF(confirmation_number <= 10 AND har_jobbet), 10),
            NULL
        ) AS share_worked_last_10,
        IF(
            COUNT(*) >= 20,
            SAFE_DIVIDE(COUNTIF(confirmation_number <= 20 AND har_jobbet), 20),
            NULL
        ) AS share_worked_last_20
    FROM RankedConfirmations
    GROUP BY correlation_id, last_end
),

DailyClosures AS (
    SELECT
        periods.last_end AS day,
        COUNT(*) AS closed_periods,
        AVG(shares.share_worked_last_2) AS average_share_worked_last_2,
        AVG(shares.share_worked_last_10) AS average_share_worked_last_10,
        AVG(shares.share_worked_last_20) AS average_share_worked_last_20,
        COUNTIF(shares.confirmation_count >= 2) AS periods_with_2_confirmations,
        COUNTIF(shares.confirmation_count >= 10) AS periods_with_10_confirmations,
        COUNTIF(shares.confirmation_count >= 20) AS periods_with_20_confirmations
    FROM ClosedPeriods periods
    LEFT JOIN SharesPerPeriod shares
        ON shares.correlation_id = periods.correlation_id
    GROUP BY periods.last_end
)

SELECT
    dates.day AS dag,
    COALESCE(closures.closed_periods, 0) AS antall_avsluttede_perioder,
    closures.average_share_worked_last_2 AS gjennomsnittlig_andel_har_jobbet_siste_2,
    closures.average_share_worked_last_10 AS gjennomsnittlig_andel_har_jobbet_siste_10,
    closures.average_share_worked_last_20 AS gjennomsnittlig_andel_har_jobbet_siste_20,
    COALESCE(closures.periods_with_2_confirmations, 0) AS antall_perioder_med_2_bekreftelser,
    COALESCE(closures.periods_with_10_confirmations, 0) AS antall_perioder_med_10_bekreftelser,
    COALESCE(closures.periods_with_20_confirmations, 0) AS antall_perioder_med_20_bekreftelser
FROM DateSeries dates
LEFT JOIN DailyClosures closures
    ON closures.day = dates.day
