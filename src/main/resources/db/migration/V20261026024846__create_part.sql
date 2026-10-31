CREATE TABLE part (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku             VARCHAR(50)    NOT NULL UNIQUE,
    name            VARCHAR(150)   NOT NULL,
    type            VARCHAR(10)    NOT NULL CHECK (type IN ('PART', 'SUPPLY')),
    unit            VARCHAR(10)    NOT NULL CHECK (unit IN ('UNIT', 'LITER', 'KG')),
    sale_price      NUMERIC(12, 2) NOT NULL CHECK (sale_price >= 0),
    stock_quantity  INTEGER        NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    minimum_stock   INTEGER        NOT NULL CHECK (minimum_stock >= 0),
    date_created    TIMESTAMPTZ    NOT NULL,
    last_updated    TIMESTAMPTZ    NOT NULL,
    version         BIGINT         NOT NULL
);
