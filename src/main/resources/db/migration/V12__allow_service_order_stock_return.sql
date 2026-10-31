ALTER TABLE stock_movement
    DROP CONSTRAINT stock_movement_created_by_user_id_check,
    ADD CONSTRAINT stock_movement_created_by_user_id_check
        CHECK (created_by_user_id IS NOT NULL OR (type IN ('IN', 'OUT') AND service_order_id IS NOT NULL));
