CREATE TABLE email_notification (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id      BIGINT        NOT NULL REFERENCES customer (id),
    type             VARCHAR(60)   NOT NULL,
    recipient        VARCHAR(254),
    subject          VARCHAR(255)  NOT NULL,
    body             TEXT          NOT NULL,
    status           VARCHAR(20)   NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts         INTEGER       NOT NULL CHECK (attempts >= 0),
    next_attempt_at  TIMESTAMPTZ,
    sent_at          TIMESTAMPTZ,
    last_error       TEXT,
    date_created     TIMESTAMPTZ   NOT NULL,
    last_updated     TIMESTAMPTZ   NOT NULL,
    version          BIGINT        NOT NULL,
    CHECK (status <> 'PENDING' OR (recipient IS NOT NULL AND next_attempt_at IS NOT NULL))
);

CREATE INDEX email_notification_pending_idx ON email_notification (next_attempt_at) WHERE status = 'PENDING';
CREATE INDEX email_notification_customer_id_idx ON email_notification (customer_id);
