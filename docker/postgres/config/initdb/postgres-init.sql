create user kafka_key_generator with password 'Paw1234';
create user bekreftelse_api with password 'Paw1234';
create user pawkafkakeymaintenance with password 'Paw1234';
create user hendelselogg_backup with password 'Paw1234';
create user paw_arbeidssoekere_synk_jobb with password 'Paw1234';

create database pawkafkakeys with owner kafka_key_generator;
create database bekreftelser with owner bekreftelse_api;
create database pawkafkakeymaintenance with owner pawkafkakeymaintenance;
create database hendelselogg_backup with owner hendelselogg_backup;
create database paw_arbeidssoekere_synk_jobb with owner paw_arbeidssoekere_synk_jobb;