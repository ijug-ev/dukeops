-- Drop table in opposite order to avoid foreign key constraints.

DROP TABLE IF EXISTS user;

-- Recreate tables in correct order.

CREATE TABLE user (
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL DEFAULT 'USER',
    CHECK (role IN ('ADMIN', 'USER')),
    PRIMARY KEY (email)
);
