create table KafkaKeys
(
    id bigint GENERATED ALWAYS AS IDENTITY,
    constraint PK_KafkaKeys primary key (id)
);

create table Identitet
(
    identitetsnummer VARCHAR(255) NOT NULL,
    kafka_key bigint NOT NULL,
    constraint PK_identitet primary key (identitetsnummer),
    constraint FK_identitet_kafkaKeys foreign key (kafka_key) references KafkaKeys (id)
);