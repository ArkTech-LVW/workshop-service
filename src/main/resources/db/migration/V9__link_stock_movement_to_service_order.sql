-- Withdrawals approved by the customer through their own API have no logged user.
ALTER TABLE stock_movement
    ADD CONSTRAINT stock_movement_service_order_id_fkey
        FOREIGN KEY (service_order_id) REFERENCES service_order (id),
    ALTER COLUMN created_by_user_id DROP NOT NULL,
    ADD CONSTRAINT stock_movement_created_by_user_id_check
        CHECK (created_by_user_id IS NOT NULL OR (type = 'OUT' AND service_order_id IS NOT NULL));

CREATE INDEX stock_movement_service_order_id_idx ON stock_movement (service_order_id);
