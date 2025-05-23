SELECT
    t1.startet.tidspunkt,
    t1.startet.brukertype,
FROM
    `arbeidssoekerregisteret_internt.perioder` AS t1
WHERE t1.startet IS NOT NULL
  AND t1.avsluttet IS NULL
  AND NOT EXISTS (
    SELECT
        1
    FROM
        `arbeidssoekerregisteret_internt.perioder` AS t2
    WHERE t1.correlation_id = t2.correlation_id
      AND t2.avsluttet IS NOT NULL
);
