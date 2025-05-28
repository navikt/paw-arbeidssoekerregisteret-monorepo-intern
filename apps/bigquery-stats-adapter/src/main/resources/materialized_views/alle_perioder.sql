WITH RankedRows AS (
    SELECT
        *,
        ROW_NUMBER() OVER (
      PARTITION BY correlation_id
      ORDER BY
        -- Prioritize rows where avsluttet is not null
        CASE WHEN avsluttet IS NOT NULL THEN 0 ELSE 1 END,
        -- If multiple rows with same nullity status, take most recent by startet.tidspunkt
        startet.tidspunkt DESC
    ) AS row_num
    FROM `arbeidssoekerregisteret_internt.perioder`
)

SELECT
    startet,
    avsluttet
FROM RankedRows
WHERE row_num = 1
