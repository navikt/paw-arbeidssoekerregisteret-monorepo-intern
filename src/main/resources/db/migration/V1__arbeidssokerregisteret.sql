CREATE TABLE IF NOT EXISTS "arbeidssokerperiode"
(
    "id"                  uuid PRIMARY KEY         NOT NULL,
    "foedselsnummer"      VARCHAR(11)              NOT NULL,
    "opprettet_tidspunkt" TIMESTAMP(6) NOT NULL DEFAULT localtimestamp,
    "endret_tidspunkt"    TIMESTAMP(6) NOT NULL DEFAULT localtimestamp,
    "fra_og_med_dato"     TIMESTAMP(6) NOT NULL,
    "til_og_med_dato"     TIMESTAMP(6),
    "begrunnelse"         VARCHAR(255),
    "opprettet_av"        VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS index_arbeidssokerperiode_foedselsnummer ON arbeidssokerperiode (foedselsnummer);
