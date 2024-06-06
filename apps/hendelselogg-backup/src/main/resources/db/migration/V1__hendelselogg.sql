CREATE TABLE hendelselogg
(
    partition smallint NOT NULL ,
    offset bigint NOT NULL,
    record_key bigint NOT NULL,
    arbeidssoeker_id bigint NOT NULL,
    data jsonb NOT NULL,
    primary key (partition, offset),
    index (arbeidssoeker_id)
);

create table hwm
(
    partition smallint NOT NULL,
    offset bigint NOT NULL,
    primary key (partition)
);
