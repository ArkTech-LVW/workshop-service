CREATE TABLE service_order_number_counter (
    year          INTEGER      PRIMARY KEY,
    last_value    BIGINT       NOT NULL CHECK (last_value > 0),
    date_created  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_updated  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version       BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE service_order (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id            UUID           NOT NULL UNIQUE,
    number               VARCHAR(20)    NOT NULL UNIQUE,
    customer_id          BIGINT         NOT NULL REFERENCES customer (id),
    vehicle_id           BIGINT         NOT NULL REFERENCES vehicle (id),
    mechanic_id          BIGINT         REFERENCES "user" (id),
    problem_description  TEXT           NOT NULL,
    status               VARCHAR(20)    NOT NULL CHECK (status IN ('RECEIVED', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL',
                                                                   'QUOTE_APPROVED', 'QUOTE_REJECTED', 'IN_PROGRESS',
                                                                   'COMPLETED', 'DELIVERED', 'CANCELED')),
    total_price          NUMERIC(12, 2) NOT NULL CHECK (total_price >= 0),
    date_created         TIMESTAMPTZ    NOT NULL,
    last_updated         TIMESTAMPTZ    NOT NULL,
    version              BIGINT         NOT NULL
);

CREATE INDEX idx_service_order_customer_id ON service_order (customer_id);
CREATE INDEX idx_service_order_vehicle_id ON service_order (vehicle_id);
CREATE INDEX idx_service_order_mechanic_id ON service_order (mechanic_id);

CREATE TABLE service_item (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    service_order_id     BIGINT         NOT NULL REFERENCES service_order (id),
    service_offering_id  BIGINT         NOT NULL REFERENCES service_offering (id),
    quantity             INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price           NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    date_created         TIMESTAMPTZ    NOT NULL,
    last_updated         TIMESTAMPTZ    NOT NULL,
    version              BIGINT         NOT NULL
);

CREATE INDEX idx_service_item_service_order_id ON service_item (service_order_id);
CREATE INDEX idx_service_item_service_offering_id ON service_item (service_offering_id);

CREATE TABLE part_item (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    service_order_id     BIGINT         NOT NULL REFERENCES service_order (id),
    part_id              BIGINT         NOT NULL REFERENCES part (id),
    quantity             INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price           NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    date_created         TIMESTAMPTZ    NOT NULL,
    last_updated         TIMESTAMPTZ    NOT NULL,
    version              BIGINT         NOT NULL
);

CREATE INDEX idx_part_item_service_order_id ON part_item (service_order_id);
CREATE INDEX idx_part_item_part_id ON part_item (part_id);

CREATE TABLE status_change (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    service_order_id     BIGINT         NOT NULL REFERENCES service_order (id),
    from_status          VARCHAR(20)    CHECK (from_status IN ('RECEIVED', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL',
                                                               'QUOTE_APPROVED', 'QUOTE_REJECTED', 'IN_PROGRESS',
                                                               'COMPLETED', 'DELIVERED', 'CANCELED')),
    to_status            VARCHAR(20)    NOT NULL CHECK (to_status IN ('RECEIVED', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL',
                                                                      'QUOTE_APPROVED', 'QUOTE_REJECTED', 'IN_PROGRESS',
                                                                      'COMPLETED', 'DELIVERED', 'CANCELED')),
    changed_by_user_id   BIGINT         REFERENCES "user" (id),
    description          TEXT,
    date_created         TIMESTAMPTZ    NOT NULL,
    last_updated         TIMESTAMPTZ    NOT NULL,
    version              BIGINT         NOT NULL
);

CREATE INDEX idx_status_change_service_order_id ON status_change (service_order_id);
