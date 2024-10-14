CREATE TABLE bekreftelser
(
    version SMALLINT NOT NULL,
    kafka_partition SMALLINT NOT NULL,
    kafka_offset BIGINT NOT NULL,
    record_key BIGINT NOT NULL,
    arbeidssoeker_id BIGINT NOT NULL,
    periode_id UUID NOT NULL,
    bekreftelse_id UUID NOT NULL UNIQUE,
    data JSONB NOT NULL,
    primary key (version, kafka_partition, kafka_offset)
);
