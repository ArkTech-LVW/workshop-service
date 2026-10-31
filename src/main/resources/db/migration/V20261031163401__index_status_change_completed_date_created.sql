CREATE INDEX idx_status_change_completed_date_created
    ON status_change (date_created) INCLUDE (service_order_id)
    WHERE to_status = 'COMPLETED';
