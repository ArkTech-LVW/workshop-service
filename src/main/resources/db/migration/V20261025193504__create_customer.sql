CREATE TABLE customer (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    document      VARCHAR(14)  NOT NULL UNIQUE,
    person_type   VARCHAR(20)  NOT NULL CHECK (person_type IN ('NATURAL_PERSON', 'LEGAL_PERSON')),
    email         VARCHAR(254) NOT NULL,
    phone         VARCHAR(11),
    date_created  TIMESTAMPTZ  NOT NULL,
    last_updated  TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL
);
