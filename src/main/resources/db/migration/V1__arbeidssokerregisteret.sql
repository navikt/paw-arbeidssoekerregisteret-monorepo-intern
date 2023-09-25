CREATE TABLE IF NOT EXISTS "arbeidssokerperiode"
(
    "id"             uuid PRIMARY KEY NOT NULL,
    "foedselsnummer" VARCHAR(11) NOT NULL,
    "opprettet_tidspunkt"      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "endret_tidspunkt"         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "fra_og_med_dato"       TIMESTAMP   NOT NULL,
    "til_og_med_dato"       TIMESTAMP,
    "begrunnelse"           VARCHAR(255),
    "opprettet_av"          VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS index_arbeidssokerperiode_foedselsnummer ON arbeidssokerperiode (foedselsnummer);
