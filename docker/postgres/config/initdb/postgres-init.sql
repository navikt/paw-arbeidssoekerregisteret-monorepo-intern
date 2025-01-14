create user pawkafkakeys with password '5up3r_53cr3t_p455w0rd';
create user bekreftelse_api with password 'Paw1234';
create user pawkafkakeymaintenance with password '5up3r_53cr3t_p455w0rd';
create user hendelselogg_backup with password '5up3r_53cr3t_p455w0rd';

create database pawkafkakeys with owner pawkafkakeys;
create database bekreftelser with owner bekreftelse_api;
create database pawkafkakeymaintenance with owner pawkafkakeymaintenance;
create database hendelselogg_backup with owner hendelselogg_backup;