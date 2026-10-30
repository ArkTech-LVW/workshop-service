ALTER TABLE service_item
    ADD COLUMN completed_at          TIMESTAMPTZ,
    ADD COLUMN completed_by_user_id  BIGINT REFERENCES "user" (id),
    ADD CONSTRAINT service_item_completion_check
        CHECK ((completed_at IS NULL) = (completed_by_user_id IS NULL));
