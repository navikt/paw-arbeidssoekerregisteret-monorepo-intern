create table hwm
(
    version smallint NOT NULL,
    kafka_topic varchar(255) NOT NULL,
    kafka_partition smallint NOT NULL,
    kafka_offset bigint NOT NULL,
    time timestamp NOT NULL,
    last_updated timestamp NOT NULL,
    primary key (version, kafka_topic, kafka_partition)
);
