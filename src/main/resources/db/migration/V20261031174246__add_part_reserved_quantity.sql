ALTER TABLE part
    ADD COLUMN reserved_quantity INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT part_reserved_quantity_check
        CHECK (reserved_quantity >= 0 AND reserved_quantity <= stock_quantity);
