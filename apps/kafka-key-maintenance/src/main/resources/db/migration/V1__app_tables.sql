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

create table perioder
(
    version smallint NOT NULL,
    periode_id uuid NOT NULL,
    identitetsnummer varchar(11) NOT NULL,
    fra timestamp NOT NULL,
    til timestamp,
    primary key (version, periode_id)
);
