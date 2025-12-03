SELECT
    avsluttet.tidspunkt AS tidspunkt,
    avsluttet.aarsak AS aarsak,
    avsluttet.brukertype AS brukertype,
    COUNT(*) AS antall_avsluttet
FROM `arbeidssoekerregisteret_internt.perioder`
WHERE avsluttet IS NOT NULL
GROUP BY avsluttet.tidspunkt, avsluttet.aarsak, avsluttet.brukertype
