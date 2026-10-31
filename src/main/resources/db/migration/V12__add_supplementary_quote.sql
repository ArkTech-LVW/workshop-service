ALTER TABLE service_order
    ALTER COLUMN status TYPE VARCHAR(40),
    DROP CONSTRAINT service_order_status_check,
    ADD CONSTRAINT service_order_status_check
        CHECK (status IN ('RECEIVED', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL', 'QUOTE_APPROVED', 'QUOTE_REJECTED',
                          'IN_PROGRESS', 'AWAITING_SUPPLEMENTARY_APPROVAL', 'SUPPLEMENTARY_APPROVED',
                          'SUPPLEMENTARY_REJECTED', 'COMPLETED', 'DELIVERED', 'CANCELED')),
    ADD COLUMN can_proceed_without_changes BOOLEAN,
    ADD COLUMN approved_item_removed       BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE status_change
    ALTER COLUMN from_status TYPE VARCHAR(40),
    ALTER COLUMN to_status TYPE VARCHAR(40),
    DROP CONSTRAINT status_change_from_status_check,
    DROP CONSTRAINT status_change_to_status_check,
    ADD CONSTRAINT status_change_from_status_check
        CHECK (from_status IN ('RECEIVED', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL', 'QUOTE_APPROVED', 'QUOTE_REJECTED',
                               'IN_PROGRESS', 'AWAITING_SUPPLEMENTARY_APPROVAL', 'SUPPLEMENTARY_APPROVED',
                               'SUPPLEMENTARY_REJECTED', 'COMPLETED', 'DELIVERED', 'CANCELED')),
    ADD CONSTRAINT status_change_to_status_check
        CHECK (to_status IN ('RECEIVED', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL', 'QUOTE_APPROVED', 'QUOTE_REJECTED',
                             'IN_PROGRESS', 'AWAITING_SUPPLEMENTARY_APPROVAL', 'SUPPLEMENTARY_APPROVED',
                             'SUPPLEMENTARY_REJECTED', 'COMPLETED', 'DELIVERED', 'CANCELED'));

ALTER TABLE service_item
    ADD COLUMN approved_at TIMESTAMPTZ;

ALTER TABLE part_item
    ADD COLUMN approved_at TIMESTAMPTZ;

ALTER TABLE stock_movement
    DROP CONSTRAINT stock_movement_created_by_user_id_check,
    ADD CONSTRAINT stock_movement_created_by_user_id_check
        CHECK (created_by_user_id IS NOT NULL OR (type IN ('IN', 'OUT') AND service_order_id IS NOT NULL));
