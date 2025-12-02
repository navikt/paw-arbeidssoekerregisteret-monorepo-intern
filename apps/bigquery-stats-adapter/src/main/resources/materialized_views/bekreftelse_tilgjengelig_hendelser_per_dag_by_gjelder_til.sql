SELECT
    tidspunkt AS dag,
    gjelder_til,
    COUNT(*) AS antall_hendelser
FROM `arbeidssoekerregisteret_internt.bekreftelse_hendelser`
WHERE hendelse = 'bekreftelse.tilgjengelig'
GROUP BY tidspunkt, gjelder_til
