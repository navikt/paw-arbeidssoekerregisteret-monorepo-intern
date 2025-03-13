CREATE TABLE eksterne_varsler
(
    varsel_id          UUID REFERENCES varsler (varsel_id),
    varsel_type        VARCHAR(50)  NOT NULL,
    varsel_status      VARCHAR(50)  NOT NULL,
    hendelse_navn      VARCHAR(50)  NOT NULL,
    hendelse_timestamp TIMESTAMP(6) NOT NULL,
    inserted_timestamp TIMESTAMP(6) NOT NULL,
    updated_timestamp  TIMESTAMP(6),
    PRIMARY KEY (varsel_id)
);