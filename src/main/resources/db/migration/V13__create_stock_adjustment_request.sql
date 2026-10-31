CREATE TABLE stock_adjustment_request (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    part_id               BIGINT        NOT NULL REFERENCES part (id),
    quantity              INTEGER       NOT NULL CHECK (quantity > 0),
    reason                VARCHAR(255)  NOT NULL,
    status                VARCHAR(10)   NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    requested_by_user_id  BIGINT        NOT NULL REFERENCES "user" (id),
    reviewed_by_user_id   BIGINT        REFERENCES "user" (id),
    review_note           VARCHAR(255),
    reviewed_at           TIMESTAMPTZ,
    date_created          TIMESTAMPTZ   NOT NULL,
    last_updated          TIMESTAMPTZ   NOT NULL,
    version               BIGINT        NOT NULL,
    CHECK ((status = 'PENDING') = (reviewed_by_user_id IS NULL AND reviewed_at IS NULL)),
    CHECK (status <> 'PENDING' OR review_note IS NULL),
    CHECK (status <> 'REJECTED' OR review_note IS NOT NULL),
    CHECK (reviewed_by_user_id IS NULL OR reviewed_by_user_id <> requested_by_user_id)
);

CREATE INDEX stock_adjustment_request_status_idx ON stock_adjustment_request (status, date_created);
CREATE INDEX stock_adjustment_request_part_id_idx ON stock_adjustment_request (part_id);

ALTER TABLE stock_movement
    ADD COLUMN stock_adjustment_request_id BIGINT UNIQUE REFERENCES stock_adjustment_request (id),
    ADD CONSTRAINT stock_movement_stock_adjustment_request_id_check
        CHECK (stock_adjustment_request_id IS NULL OR type = 'ADJUSTMENT');
