CREATE TABLE konflikter
(
    id                 BIGSERIAL PRIMARY KEY,
    aktor_id           VARCHAR(50)  NOT NULL,
    type               VARCHAR(50)  NOT NULL,
    status             VARCHAR(50)  NOT NULL,
    source_timestamp   TIMESTAMP(6) NOT NULL,
    inserted_timestamp TIMESTAMP(6) NOT NULL,
    updated_timestamp  TIMESTAMP(6)
);

CREATE TABLE konflikt_identiteter
(
    id                 BIGSERIAL PRIMARY KEY,
    konflikt_id        BIGSERIAL    NOT NULL REFERENCES konflikter (id),
    identitet          VARCHAR(50)  NOT NULL,
    type               VARCHAR(50)  NOT NULL,
    gjeldende          BOOLEAN      NOT NULL,
    inserted_timestamp TIMESTAMP(6) NOT NULL,
    updated_timestamp  TIMESTAMP(6),
    UNIQUE (konflikt_id, identitet)
);
