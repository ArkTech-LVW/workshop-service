CREATE TABLE service_offering (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name               VARCHAR(150)   NOT NULL,
    description        TEXT,
    labor_price        NUMERIC(12, 2) NOT NULL CHECK (labor_price > 0),
    estimated_minutes  INTEGER        NOT NULL CHECK (estimated_minutes > 0),
    active             BOOLEAN        NOT NULL DEFAULT TRUE,
    date_created       TIMESTAMPTZ    NOT NULL,
    last_updated       TIMESTAMPTZ    NOT NULL,
    version            BIGINT         NOT NULL
);
