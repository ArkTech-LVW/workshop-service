CREATE TABLE vehicle (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id    BIGINT       NOT NULL REFERENCES customer (id),
    license_plate  VARCHAR(7)   NOT NULL UNIQUE,
    chassis        VARCHAR(17)  UNIQUE,
    brand          VARCHAR(50)  NOT NULL,
    model          VARCHAR(80)  NOT NULL,
    year           INTEGER      NOT NULL CHECK (year >= 1981),
    color          VARCHAR(30)  NOT NULL,
    date_created   TIMESTAMPTZ  NOT NULL,
    last_updated   TIMESTAMPTZ  NOT NULL,
    version        BIGINT       NOT NULL
);

CREATE INDEX idx_vehicle_customer_id ON vehicle (customer_id);
