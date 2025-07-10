CREATE TABLE hendelser
(
    id                 BIGSERIAL PRIMARY KEY,
    arbeidssoeker_id   BIGINT       NOT NULL REFERENCES KafkaKeys (id),
    aktor_id           VARCHAR(50)  NOT NULL,
    version            SMALLINT     NOT NULL,
    data               JSONB        NOT NULL,
    kafka_partition    SMALLINT,
    kafka_offset       BIGINT,
    status             VARCHAR(50)  NOT NULL,
    inserted_timestamp TIMESTAMP(6) NOT NULL,
    updated_timestamp  TIMESTAMP(6)
);
