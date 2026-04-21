WITH
LatestPerPeriode AS (
    SELECT
        correlation_id,
        startet.tidspunkt AS startet_tidspunkt,
        startet.brukertype AS brukertype,
        avsluttet.tidspunkt AS avsluttet_tidspunkt
    FROM (
        SELECT
            *,
            ROW_NUMBER() OVER (
                PARTITION BY correlation_id
                ORDER BY
                    CASE WHEN avsluttet IS NOT NULL THEN 0 ELSE 1 END,
                    startet.tidspunkt DESC,
                    avsluttet.tidspunkt DESC NULLS LAST
            ) AS row_num
        FROM `arbeidssoekerregisteret_internt.perioder`
    )
    WHERE row_num = 1
      AND avsluttet IS NOT NULL
),
-- Join to hendelser to get person ID and opplysning flags from the Startet event
PerioderMedHendelse AS (
    SELECT
        lp.correlation_id,
        h.id AS person_id,
        IFNULL(h.options, CAST([] AS ARRAY<STRING>)) AS options,
        lp.startet_tidspunkt,
        lp.brukertype,
        DATE_DIFF(lp.avsluttet_tidspunkt, lp.startet_tidspunkt, DAY) AS varighet_dager
    FROM LatestPerPeriode lp
    INNER JOIN `arbeidssoekerregisteret_internt.hendelser` h
        ON lp.correlation_id = h.correlation_id
        AND h.type = 'intern.v1.startet'
),
-- Determine period rank per person to flag repeat registrants
PerioderMedRank AS (
    SELECT
        correlation_id,
        person_id,
        brukertype,
        options,
        varighet_dager,
        ROW_NUMBER() OVER (
            PARTITION BY person_id
            ORDER BY startet_tidspunkt, correlation_id
        ) AS periode_nr
    FROM PerioderMedHendelse
)
SELECT
    brukertype,
    'er_norsk_statsborger' IN UNNEST(options) AS er_norsk,
    'er_eu_eoes_statsborger' IN UNNEST(options) AS er_eu_eoes,
    'bosatt_etter_freg_loven' IN UNNEST(options) AS bosatt_etter_freg,
    'dnummer' IN UNNEST(options) AS dnummer,
    'siste_flytting_var_inn_til_norge' IN UNNEST(options) AS siste_inn_til_norge,
    'har_gyldig_oppholdstillatelse' IN UNNEST(options) AS har_oppholdstillatelse,
    (periode_nr > 1) AS er_gjentakende_registrant,
    COUNT(*) AS antall,
    COUNTIF(varighet_dager <= 182) AS antall_korttid,
    COUNTIF(varighet_dager > 182) AS antall_langtid,
    SAFE_DIVIDE(COUNTIF(varighet_dager <= 182), COUNT(*)) AS andel_korttid
FROM PerioderMedRank
WHERE varighet_dager >= 0
GROUP BY
    brukertype,
    er_norsk,
    er_eu_eoes,
    bosatt_etter_freg,
    dnummer,
    siste_inn_til_norge,
    har_oppholdstillatelse,
    er_gjentakende_registrant
