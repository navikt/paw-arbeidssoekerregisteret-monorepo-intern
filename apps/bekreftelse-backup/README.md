# Hendelselogg backup
Denne applikasjonen lagrer forløpende innholdet i topicene 'paw.arbeidssoker-bekreftelse-hendelseslogg-v2', 'paw.arbeidssoker-bekreftelse-v1' og 'paw.arbeidssoker-bekreftelse-paavegneav-v2' til postgres. Dette gjøres for å sikre oss mot feilaktig sletting av alt eller deler av innholdet i denne topicen. NAIS platformen kjører daglig backup av Postgres datarbaser.

## Løsnings beskrivelse
Applikasjonen kjører uten å committe offset til kafka. Istedenfor brukes en egen HWM tabell som som oppdateres i samme transaksjon som dataen blir skrevet til databasen. Det er også lagt inn støtte for versjonering slik at ved å endre 'InitApplication.CURRENT_VERSION' vil løsningen starte på nytt og lagre data på nytt uten å røre eksisterende data.

### Database oversikt:

```sql
CREATE TABLE bekreftelse_hendelser
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

CREATE TABLE bekreftelser
(
    version smallint NOT NULL,
    kafka_partition smallint NOT NULL,
    kafka_offset bigint NOT NULL,
    record_key bigint NOT NULL,
    traceparent varchar(58),
    data bytea NOT NULL,
    primary key (version, kafka_partition, kafka_offset)
);

CREATE TABLE bekreftelse_paa_vegne_av
(
    version smallint NOT NULL,
    kafka_partition smallint NOT NULL,
    kafka_offset bigint NOT NULL,
    record_key bigint NOT NULL,
    traceparent varchar(58),
    data bytea NOT NULL,
    primary key (version, kafka_partition, kafka_offset)
);

CREATE TABLE bekreftelse_hwm
(
    version smallint NOT NULL,
    kafka_partition smallint NOT NULL,
    kafka_offset bigint NOT NULL,
    kafka_topic varchar(255) NOT NULL,
    primary key (version, kafka_partition, kafka_topic)
);

-- indexes
CREATE INDEX bekreftelse_hendelser_data_idx ON bekreftelse_hendelser USING GIN (data jsonb_path_ops);
CREATE INDEX bekreftelse_hendelser_arbeidssoeker_id_idx ON bekreftelse_hendelser (arbeidssoeker_id);

CREATE INDEX bekreftelser_data_idx ON bekreftelser USING GIN (data bytea_ops);
CREATE INDEX bekreftelse_paa_vegne_av_data_idx ON bekreftelse_paa_vegne_av USING GIN (data bytea_ops);

```
