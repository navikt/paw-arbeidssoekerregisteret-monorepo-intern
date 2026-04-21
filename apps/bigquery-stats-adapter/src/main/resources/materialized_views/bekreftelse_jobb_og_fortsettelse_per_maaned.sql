SELECT
    FORMAT_DATE('%Y-%m', tidspunkt) AS maaned,
    loesning,
    brukertype,
    COUNT(*) AS antall_bekreftelses,
    COUNTIF(har_jobbet) AS antall_har_jobbet,
    COUNTIF(NOT vil_fortsette) AS antall_vil_ikke_fortsette,
    COUNTIF(har_jobbet AND NOT vil_fortsette) AS antall_har_jobb_og_slutter,
    COUNTIF(NOT har_jobbet AND NOT vil_fortsette) AS antall_ingen_jobb_og_slutter,
    SAFE_DIVIDE(COUNTIF(har_jobbet), COUNT(*)) AS andel_har_jobbet,
    SAFE_DIVIDE(COUNTIF(NOT vil_fortsette), COUNT(*)) AS andel_vil_ikke_fortsette
FROM `arbeidssoekerregisteret_internt.bekreftelser`
GROUP BY maaned, loesning, brukertype
