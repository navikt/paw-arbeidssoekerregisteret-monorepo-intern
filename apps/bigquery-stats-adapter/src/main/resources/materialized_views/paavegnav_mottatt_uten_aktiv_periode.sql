WITH
-- Get the latest status for each correlation_id at any point in time
PeriodeStatus AS (
    SELECT
        correlation_id,
        MIN(startet.tidspunkt) AS first_start,
        MAX(CASE WHEN avsluttet IS NOT NULL THEN avsluttet.tidspunkt ELSE NULL END) AS last_end
    FROM `arbeidssoekerregisteret_internt.perioder`
    GROUP BY correlation_id
),

-- Count all paa-vegne-av records by grouping dimensions
TotalCounts AS (
    SELECT
        tidspunkt,
        loesning,
        handling,
        frist_brutt,
        COUNT(*) AS total_antall
    FROM `arbeidssoekerregisteret_internt.paavnegneav`
    GROUP BY tidspunkt, loesning, handling, frist_brutt
)

-- Count paa-vegne-av records where period was not active at the time
SELECT
    pva.tidspunkt,
    pva.loesning,
    pva.handling,
    pva.frist_brutt,
    COUNT(*) AS antall_mottatt_uten_aktiv_periode,
    MAX(tc.total_antall) AS total_antall
FROM `arbeidssoekerregisteret_internt.paavnegneav` pva
LEFT JOIN PeriodeStatus ps
    ON pva.correlation_id = ps.correlation_id
INNER JOIN TotalCounts tc
    ON pva.tidspunkt = tc.tidspunkt
    AND pva.loesning = tc.loesning
    AND pva.handling = tc.handling
    AND (pva.frist_brutt = tc.frist_brutt OR (pva.frist_brutt IS NULL AND tc.frist_brutt IS NULL))
WHERE 
    -- Period was not active at the time (either not started yet or already ended)
    ps.correlation_id IS NULL
    OR pva.tidspunkt < ps.first_start
    OR (ps.last_end IS NOT NULL AND pva.tidspunkt > ps.last_end)
GROUP BY pva.tidspunkt, pva.loesning, pva.handling, pva.frist_brutt
