ALTER TABLE varsler ADD COLUMN bekreftelse_id UUID UNIQUE;
CREATE INDEX varsler_bekreftelse_id_idx ON varsler (bekreftelse_id);