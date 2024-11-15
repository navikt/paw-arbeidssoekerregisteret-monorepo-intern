create table data
(
    version smallint NOT NULL,
    id varchar(255) NOT NULL,
    traceparant bytea NOT NULL,
    time timestamp NOT NULL,
    data bytea NOT NULL,
    primary key (version, id)
);

CREATE INDEX data_time_idx ON data(time);
