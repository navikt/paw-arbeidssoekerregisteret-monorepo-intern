# Hendelselogg backup
Denne applikasjonen lagrer forløpende innholdet i topic 'paw.arbeidssoker-hendelseslogg-v1' til postgres. Dette gjøres for å sikre oss mot feilaktig sletting av alt eller deler av innholdet i denne topicen. NAIS platformen kjører daglig backup av Postgres datarbaser.

## Løsnings beskrivelse
Applikasjonen kjører uten å committe offset til kafka. Istedenfor brukes en egen HWM tabell som som oppdateres i samme transaksjon som dataen blir skrevet til databasen. Det er også lagt inn støtte for versjonering slik at ved å endre 'InitApplication.CURRENT_VERSION' vil løsningen starte på nytt og lagre data på nytt uten å røre eksisterende data.

### Database oversikt:

```sql
CREATE TABLE hendelser
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

create table hwm
(
    version smallint NOT NULL,
    kafka_partition smallint NOT NULL,
    kafka_offset bigint NOT NULL,
    primary key (version, kafka_partition)
);

-- indexes
CREATE index hendelser_data_idx ON hendelser USING GIN (data jsonb_path_ops);

```

# Andre bruksområder
Tabellen 'hendelser' kan brukes å generere diverse oversikter og rapporter over hendelsene som har blitt lagret.
## Eksempler:

### EUS/EØS borgere:
- Antall personer som har blitt forhåndsgodkjent:
 ```sql
 select count(distinct arbeidssoeker_id) from hendelser 
  where (data @> '{"hendelseType": "intern.v1.startet"}' and 
    ((data->'opplysninger')::jsonb ? 'FORHAANDSGODKJENT_AV_ANSATT')
  );
 ```

- Antall Norske statsborgere som er avvist etter 1722928759.442 grunnet 'IKKE_BOSATT' og hvor vi ikke finner en utenlansk adresse i PDL.
 ```sql
 select count(distinct arbeidssoeker_id) from hendelser 
  where (data @> '{"hendelseType": "intern.v1.avvist"}' and 
    ((data ->'opplysninger')::jsonb ? 'ER_NORSK_STATSBORGER') and
    NOT ((data->'opplysninger')::jsonb ? 'HAR_UTENLANDSK_ADRESSE') and
    ((data->'opplysninger')::jsonb ? 'IKKE_BOSATT') and
    (data @> '{"metadata": {"utfoertAv": {"type":"SLUTTBRUKER"}}}') and
    (data #>> '{metadata,tidspunkt}') > '1722928759.442'
  );
 ```
Samme som over, men hvor vi finner en utenlandsk adresse i PDL.
```sql
 select count(distinct arbeidssoeker_id) from hendelser 
  where (data @> '{"hendelseType": "intern.v1.avvist"}' and 
    ((data ->'opplysninger')::jsonb ? 'ER_NORSK_STATSBORGER') and
    ((data->'opplysninger')::jsonb ? 'HAR_UTENLANDSK_ADRESSE') and
    ((data->'opplysninger')::jsonb ? 'IKKE_BOSATT') and
    (data @> '{"metadata": {"utfoertAv": {"type":"SLUTTBRUKER"}}}') and
    (data #>> '{metadata,tidspunkt}') > '1722928759.442'
  );
 ```

- Antall EU/EØS statsborgere (eksludert Norske statsborgere) som er avvist etter 1722928759.442, hvor vi finner en utenlandsk adresse.
 ```sql
 select count(distinct arbeidssoeker_id) from hendelser 
  where (data @> '{"hendelseType": "intern.v1.avvist"}' and 
    NOT ((data ->'opplysninger')::jsonb ? 'ER_NORSK_STATSBORGER') and
    ((data ->'opplysninger')::jsonb ? 'ER_EU_EOES_STATSBORGER') and
    ((data->'opplysninger')::jsonb ? 'HAR_UTENLANDSK_ADRESSE') and
    (data @> '{"metadata": {"utfoertAv": {"type":"SLUTTBRUKER"}}}') and
    (data #>> '{metadata,tidspunkt}') > '1722928759.442'
  );
```

- Antall EU/EØS statsborgere (eksludert Norske statsborgere) som er avvist etter 1722928759.442, hvor vi *ikke* finner en utenlandsk adresse.
```sql
 select count(distinct arbeidssoeker_id) from hendelser 
  where (data @> '{"hendelseType": "intern.v1.avvist"}' and 
    NOT ((data ->'opplysninger')::jsonb ? 'ER_NORSK_STATSBORGER') and
    ((data ->'opplysninger')::jsonb ? 'ER_EU_EOES_STATSBORGER') and
    NOT ((data->'opplysninger')::jsonb ? 'HAR_UTENLANDSK_ADRESSE') and
    (data @> '{"metadata": {"utfoertAv": {"type":"SLUTTBRUKER"}}}') and
    (data #>> '{metadata,tidspunkt}') > '1722928759.442'
  );
```
