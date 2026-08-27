WITH DataDates AS (
    SELECT startet.tidspunkt AS day
    FROM `arbeidssoekerregisteret_internt.perioder`

    UNION ALL

    SELECT avsluttet.tidspunkt AS day
    FROM `arbeidssoekerregisteret_internt.perioder`
    WHERE avsluttet IS NOT NULL

    UNION ALL

    SELECT tidspunkt AS day
    FROM `arbeidssoekerregisteret_internt.bekreftelser`
    WHERE tidspunkt >= DATE '2025-09-01'

    UNION ALL

    SELECT gjelder_til AS day
    FROM `arbeidssoekerregisteret_internt.bekreftelser`
    WHERE tidspunkt >= DATE '2025-09-01'
),

DateBounds AS (
    SELECT
        GREATEST(
            COALESCE(MAX(day), DATE '2025-09-01'),
            DATE '2025-09-01'
        ) AS last_day
    FROM DataDates
),

DateSeries AS (
    SELECT day
    FROM DateBounds
    CROSS JOIN UNNEST(
        GENERATE_DATE_ARRAY(DATE '2025-09-01', last_day)
    ) AS day
),

LatestPeriods AS (
    SELECT
        correlation_id,
        MIN(startet.tidspunkt) AS first_start,
        MAX(CASE WHEN avsluttet IS NOT NULL THEN avsluttet.tidspunkt END) AS last_end
    FROM `arbeidssoekerregisteret_internt.perioder`
    GROUP BY correlation_id
),

ActivePeriodDays AS (
    SELECT
        dates.day,
        periods.correlation_id
    FROM DateSeries dates
    INNER JOIN LatestPeriods periods
        ON periods.first_start <= dates.day
        AND (periods.last_end IS NULL OR periods.last_end > dates.day)
),

ConfirmationDays AS (
    SELECT
        confirmations.correlation_id,
        dates.day,
        confirmations.tidspunkt,
        confirmations.gjelder_fra,
        confirmations.gjelder_til,
        IF(confirmations.har_jobbet, 'JA', 'NEI') AS har_jobbet
    FROM `arbeidssoekerregisteret_internt.bekreftelser` confirmations
    INNER JOIN DateSeries dates
        ON dates.day BETWEEN confirmations.gjelder_fra AND confirmations.gjelder_til
    WHERE confirmations.tidspunkt >= DATE '2025-09-01'
),

LatestConfirmationPerDay AS (
    SELECT
        correlation_id,
        day,
        har_jobbet
    FROM (
        SELECT
            *,
            ROW_NUMBER() OVER (
                PARTITION BY correlation_id, day
                ORDER BY tidspunkt DESC, gjelder_til DESC, gjelder_fra DESC
            ) AS row_num
        FROM ConfirmationDays
    )
    WHERE row_num = 1
),

DailyCounts AS (
    SELECT
        periods.day,
        COALESCE(confirmations.har_jobbet, 'UKJENT') AS har_jobbet,
        COUNT(*) AS active_periods
    FROM ActivePeriodDays periods
    LEFT JOIN LatestConfirmationPerDay confirmations
        ON confirmations.correlation_id = periods.correlation_id
        AND confirmations.day = periods.day
    GROUP BY periods.day, har_jobbet
),

Statuses AS (
    SELECT 'JA' AS har_jobbet
    UNION ALL
    SELECT 'NEI'
    UNION ALL
    SELECT 'UKJENT'
)

SELECT
    dates.day AS dag,
    statuses.har_jobbet,
    COALESCE(counts.active_periods, 0) AS antall_aktive_perioder
FROM DateSeries dates
CROSS JOIN Statuses statuses
LEFT JOIN DailyCounts counts
    ON counts.day = dates.day
    AND counts.har_jobbet = statuses.har_jobbet
