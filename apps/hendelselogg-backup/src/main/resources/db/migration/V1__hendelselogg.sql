CREATE TABLE hendelser
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

create table hwm
(
    version smallint NOT NULL,
    kafka_partition smallint NOT NULL,
    kafka_offset bigint NOT NULL,
    primary key (version, kafka_partition)
);
