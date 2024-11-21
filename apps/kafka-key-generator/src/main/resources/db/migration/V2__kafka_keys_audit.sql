create table kafka_keys_audit
(
    id bigint GENERATED ALWAYS AS IDENTITY,
    identitetsnummer VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    detaljer VARCHAR(255),
    tidspunkt TIMESTAMP(6) NOT NULL,
    constraint kafka_keys_audit_pk primary key (id),
    constraint kafka_keys_audit_identitet_fk foreign key (identitetsnummer) references Identitet (identitetsnummer)
);