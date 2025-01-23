drop table data;

create table person_table (
  versjon SMALLINT NOT NULL,
  id BIGSERIAL PRIMARY KEY,
  record_key VARCHAR(20) NOT NULL,
  sist_endret TIMESTAMP NOT NULL,
  tidspunkt_fra_kilde TIMESTAMP NOT NULL,
  traceparent varchar(55) NOT NULL,
  merge_prosessert BOOLEAN NOT NULL
);
create index idx_person_table_versjon on person_table(versjon);
create index idx_person_table_merge_prosessert on person_table(merge_prosessert);
create index idx_person_table_sid_endret on person_table(sist_endret);

create table ident_table (
       person_id BIGINT NOT NULL,
       ident varchar(20) NOT NULL,
       ident_type smallint NOT NULL,
       gjeldende boolean NOT NULL,
         primary key (person_id, ident, ident_type),
            foreign key (person_id) references person_table(id) on delete cascade
);

