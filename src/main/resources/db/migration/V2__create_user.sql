CREATE TABLE "user" (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    date_created  TIMESTAMPTZ  NOT NULL,
    last_updated  TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL
);

CREATE TABLE user_role (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES "user" (id),
    role         VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'ATTENDANT', 'MECHANIC')),
    date_created TIMESTAMPTZ NOT NULL,
    last_updated TIMESTAMPTZ NOT NULL,
    version      BIGINT      NOT NULL,
    UNIQUE (user_id, role)
);
