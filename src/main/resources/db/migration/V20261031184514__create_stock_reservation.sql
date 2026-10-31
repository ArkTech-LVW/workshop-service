CREATE TABLE stock_reservation (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    part_id           BIGINT       NOT NULL REFERENCES part (id),
    service_order_id  BIGINT       NOT NULL REFERENCES service_order (id),
    quantity          INTEGER      NOT NULL CHECK (quantity > 0),
    expires_at        TIMESTAMPTZ  NOT NULL,
    date_created      TIMESTAMPTZ  NOT NULL,
    last_updated      TIMESTAMPTZ  NOT NULL,
    version           BIGINT       NOT NULL,
    UNIQUE (service_order_id, part_id)
);

CREATE INDEX stock_reservation_part_id_idx ON stock_reservation (part_id);
CREATE INDEX stock_reservation_expires_at_idx ON stock_reservation (expires_at);
