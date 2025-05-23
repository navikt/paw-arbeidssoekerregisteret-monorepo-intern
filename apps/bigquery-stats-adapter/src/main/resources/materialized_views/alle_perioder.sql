WITH FirstStartet AS (
    SELECT
        correlation_id,
        startet
    FROM
        `arbeidssoekerregisteret_internt.perioder`
    WHERE
        startet IS NOT NULL
    QUALIFY
    ROW_NUMBER() OVER (PARTITION BY correlation_id ORDER BY startet.tidspunkt ASC) = 1
    ),

    FirstAvsluttet AS (
SELECT
    correlation_id,
    avsluttet
FROM
    `arbeidssoekerregisteret_internt.perioder`
WHERE
    avsluttet IS NOT NULL
    QUALIFY
    ROW_NUMBER() OVER (PARTITION BY correlation_id ORDER BY avsluttet.tidspunkt ASC) = 1
    )

SELECT
    s.startet,
    a.avsluttet
FROM
    FirstStartet s
        LEFT JOIN
    FirstAvsluttet a
    ON
        s.correlation_id = a.correlation_id
