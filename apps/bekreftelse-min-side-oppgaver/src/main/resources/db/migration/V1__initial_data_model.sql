CREATE TABLE perioder
(
    periode_id          UUID UNIQUE  NOT NULL,
    identitetsnummer    VARCHAR(20)  NOT NULL,
    startet_timestamp   TIMESTAMP(6) NOT NULL,
    avsluttet_timestamp TIMESTAMP(6),
    inserted_timestamp  TIMESTAMP(6) NOT NULL,
    updated_timestamp   TIMESTAMP(6),
    PRIMARY KEY (periode_id)
);

CREATE TABLE varsler
(
    periode_id         UUID REFERENCES perioder (periode_id),
    bekreftelse_id     UUID UNIQUE  NOT NULL,
    varsel_type        VARCHAR(50)  NOT NULL,
    varsel_status      VARCHAR(50)  NOT NULL,
    hendelse_navn      VARCHAR(50)  NOT NULL,
    hendelse_timestamp TIMESTAMP(6) NOT NULL,
    inserted_timestamp TIMESTAMP(6) NOT NULL,
    updated_timestamp  TIMESTAMP(6),
    PRIMARY KEY (periode_id, bekreftelse_id)
);
