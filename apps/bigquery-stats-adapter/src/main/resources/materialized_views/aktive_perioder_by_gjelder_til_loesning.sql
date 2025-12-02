WITH
-- Get active periods (those where no row exists with avsluttet set)
    ActivePeriods AS (
        SELECT
            correlation_id
        FROM `arbeidssoekerregisteret_internt.perioder`
        GROUP BY correlation_id
        HAVING MAX(CASE WHEN avsluttet IS NOT NULL THEN 1 ELSE 0 END) = 0
    ),

-- Get the latest bekreftelse for each active period
    LatestBekreftelse AS (
        SELECT
            b.correlation_id,
            b.gjelder_til,
            b.loesning,
            ROW_NUMBER() OVER (
                PARTITION BY b.correlation_id
                ORDER BY b.tidspunkt DESC, b.gjelder_til DESC
            ) AS row_num
        FROM `arbeidssoekerregisteret_internt.bekreftelser` b
        INNER JOIN ActivePeriods ap
            ON b.correlation_id = ap.correlation_id
    )

-- Count active periods by gjelder_til and loesning
SELECT
    gjelder_til,
    loesning,
    COUNT(DISTINCT correlation_id) AS active_count
FROM LatestBekreftelse
WHERE row_num = 1
GROUP BY gjelder_til, loesning
