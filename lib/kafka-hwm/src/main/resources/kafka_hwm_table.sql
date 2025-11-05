CREATE TABLE kafka_hwm
(
    version           SMALLINT     NOT NULL,
    kafka_topic       VARCHAR(255) NOT NULL,
    kafka_partition   SMALLINT     NOT NULL,
    kafka_offset      BIGINT       NOT NULL,
    timestamp         TIMESTAMP    NOT NULL,
    updated_timestamp TIMESTAMP    NOT NULL,
    PRIMARY KEY (version, kafka_topic, kafka_partition)
);
