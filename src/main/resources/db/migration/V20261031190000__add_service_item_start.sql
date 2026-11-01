ALTER TABLE service_item
    ADD COLUMN started_at          TIMESTAMPTZ,
    ADD COLUMN started_by_user_id  BIGINT REFERENCES "user" (id);

UPDATE service_item
SET started_at = completed_at,
    started_by_user_id = completed_by_user_id
WHERE completed_at IS NOT NULL;

ALTER TABLE service_item
    ADD CONSTRAINT service_item_start_check
        CHECK ((started_at IS NULL) = (started_by_user_id IS NULL)),
    ADD CONSTRAINT service_item_completed_after_start_check
        CHECK (completed_at IS NULL OR started_at IS NOT NULL);
