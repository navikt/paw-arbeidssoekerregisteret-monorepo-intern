CREATE TABLE arbeidssoekere_synk
(
    version          VARCHAR(20)  NOT NULL,
    identitetsnummer VARCHAR(20)  NOT NULL,
    status           INTEGER      NOT NULL,
    inserted         TIMESTAMP(6) NOT NULL,
    updated          TIMESTAMP(6),
    PRIMARY KEY (version, identitetsnummer)
);
