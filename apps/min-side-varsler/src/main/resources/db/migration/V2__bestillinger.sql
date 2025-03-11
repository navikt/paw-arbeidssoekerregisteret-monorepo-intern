CREATE TABLE bestillinger
(
    bestilling_id      UUID UNIQUE  NOT NULL,
    bestiller          VARCHAR(50)  NOT NULL,
    status             VARCHAR(50)  NOT NULL,
    inserted_timestamp TIMESTAMP(6) NOT NULL,
    updated_timestamp  TIMESTAMP(6),
    PRIMARY KEY (bestilling_id)
);

CREATE TABLE bestilte_varsler
(
    bestilling_id      UUID REFERENCES bestillinger (bestilling_id),
    periode_id         UUID         NOT NULL,
    varsel_id          UUID UNIQUE  NOT NULL,
    identitetsnummer   VARCHAR(20)  NOT NULL,
    status             VARCHAR(50)  NOT NULL,
    inserted_timestamp TIMESTAMP(6) NOT NULL,
    updated_timestamp  TIMESTAMP(6),
    PRIMARY KEY (varsel_id)
);
