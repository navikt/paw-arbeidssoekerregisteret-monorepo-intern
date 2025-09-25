SELECT
    EXTRACT(YEAR FROM metadata.tidspunkt) AS year,
  EXTRACT(MONTH FROM metadata.tidspunkt) AS month,
  'er_under_18_aar' IN UNNEST(options) as under_18_aar,
  'er_norsk_statsborger' IN UNNEST(options) as er_norsk,
  'er_eu_eoes_statsborger' NOT IN UNNEST(options) as ikke_eu,
  'dnummer' IN UNNEST(options) as dnummer,
  'doed' IN UNNEST(options) as doed,
  'savnet' IN UNNEST(options) as savnet,
  'opphoert_identitet' IN UNNEST(options) opphoert_id,
  COUNT(DISTINCT id) AS distinct_id_count
FROM
    `arbeidssoekerregisteret_internt.hendelser`
WHERE
    type = 'intern.v1.avvist'
GROUP BY
    year,
    month,
    under_18_aar,
    er_norsk,
    ikke_eu,
    dnummer,
    doed,
    savnet,
    opphoert_id;