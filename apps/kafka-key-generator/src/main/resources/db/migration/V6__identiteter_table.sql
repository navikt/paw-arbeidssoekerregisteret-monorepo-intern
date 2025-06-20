CREATE TABLE identiteter
(
    id                 BIGSERIAL PRIMARY KEY,
    arbeidssoeker_id   BIGINT       NOT NULL REFERENCES KafkaKeys (id),
    aktor_id           VARCHAR(50)  NOT NULL,
    identitet          VARCHAR(50)  NOT NULL,
    type               VARCHAR(50)  NOT NULL,
    gjeldende          BOOLEAN      NOT NULL,
    status             VARCHAR(50)  NOT NULL,
    source_timestamp   TIMESTAMP(6) NOT NULL,
    inserted_timestamp TIMESTAMP(6) NOT NULL,
    updated_timestamp  TIMESTAMP(6),
    UNIQUE (aktor_id, identitet)
);
