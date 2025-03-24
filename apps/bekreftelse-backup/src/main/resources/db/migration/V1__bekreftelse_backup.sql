CREATE TABLE bekreftelse_hendelser
(
    version smallint NOT NULL,
    kafka_partition smallint NOT NULL,
    kafka_offset bigint NOT NULL,
    record_key bigint NOT NULL,
    arbeidssoeker_id bigint NOT NULL,
    traceparent varchar(58),
    data jsonb NOT NULL,
    primary key (version, kafka_partition, kafka_offset)
);

CREATE TABLE bekreftelser
(
    version smallint NOT NULL,
    kafka_partition smallint NOT NULL,
    kafka_offset bigint NOT NULL,
    record_key bigint NOT NULL,
    traceparent varchar(58),
    data bytea NOT NULL,
    primary key (version, kafka_partition, kafka_offset)
);

CREATE TABLE bekreftelse_paa_vegne_av
(
    version smallint NOT NULL,
    kafka_partition smallint NOT NULL,
    kafka_offset bigint NOT NULL,
    record_key bigint NOT NULL,
    traceparent varchar(58),
    data bytea NOT NULL,
    primary key (version, kafka_partition, kafka_offset)
);

CREATE TABLE bekreftelse_hwm
(
    version smallint NOT NULL,
    kafka_partition smallint NOT NULL,
    kafka_offset bigint NOT NULL,
    kafka_topic varchar(255) NOT NULL,
    primary key (version, kafka_partition, kafka_topic)
);

CREATE INDEX bekreftelse_hendelser_data_idx ON bekreftelse_hendelser USING GIN (data jsonb_path_ops);
CREATE INDEX bekreftelse_hendelser_arbeidssoeker_id_idx ON bekreftelse_hendelser (arbeidssoeker_id);
