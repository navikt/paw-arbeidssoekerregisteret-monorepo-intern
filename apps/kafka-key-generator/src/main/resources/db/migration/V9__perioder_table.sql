CREATE TABLE perioder
(
    id                  BIGSERIAL PRIMARY KEY,
    periode_id          UUID         NOT NULL,
    identitet           VARCHAR(50)  NOT NULL,
    startet_timestamp   TIMESTAMP(6) NOT NULL,
    avsluttet_timestamp TIMESTAMP(6),
    source_timestamp    TIMESTAMP(6) NOT NULL,
    inserted_timestamp  TIMESTAMP(6) NOT NULL,
    updated_timestamp   TIMESTAMP(6),
    UNIQUE (periode_id)
);
