CREATE TABLE perioder
(
    periode_id          UUID PRIMARY KEY,
    identitet           VARCHAR(50)  NOT NULL,
    startet_timestamp   TIMESTAMP(6) NOT NULL,
    avsluttet_timestamp TIMESTAMP(6),
    source_timestamp    TIMESTAMP(6) NOT NULL,
    inserted_timestamp  TIMESTAMP(6) NOT NULL,
    updated_timestamp   TIMESTAMP(6)
);
