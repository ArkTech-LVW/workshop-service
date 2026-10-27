CREATE TABLE stock_movement (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    part_id          BIGINT       NOT NULL REFERENCES part (id),
    service_order_id BIGINT,
    type             VARCHAR(10)  NOT NULL CHECK (type IN ('IN', 'OUT', 'ADJUSTMENT')),
    quantity         INTEGER      NOT NULL CHECK (quantity > 0),
    reason           VARCHAR(255),
    created_by       BIGINT       NOT NULL REFERENCES "user" (id),
    date_created     TIMESTAMPTZ  NOT NULL,
    last_updated     TIMESTAMPTZ  NOT NULL,
    version          BIGINT       NOT NULL,
    CHECK (type <> 'ADJUSTMENT' OR reason IS NOT NULL)
);

CREATE INDEX stock_movement_part_id_idx ON stock_movement (part_id, date_created DESC);
