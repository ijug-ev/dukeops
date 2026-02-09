-- Drop table in opposite order to avoid foreign key constraints.

DROP TABLE IF EXISTS clubdesk;
DROP TABLE IF EXISTS user;

-- Recreate tables in correct order.

CREATE TABLE user (
    id VARCHAR(36) NOT NULL,
    created TIMESTAMP NOT NULL,
    updated TIMESTAMP NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL DEFAULT 'USER',
    CHECK (role IN ('ADMIN', 'USER')),
    PRIMARY KEY (id),
    UNIQUE uk_user_email (email)
);

CREATE TABLE clubdesk (
    id VARCHAR(36) NOT NULL,

    created TIMESTAMP NOT NULL,
    updated TIMESTAMP NOT NULL,
    
    firstname VARCHAR(255) NOT NULL DEFAULT '',
    lastname VARCHAR(255) NOT NULL DEFAULT '',
    address VARCHAR(255) NOT NULL DEFAULT '',
    address_addition VARCHAR(255) NOT NULL DEFAULT '',
    zip_code VARCHAR(255) NOT NULL DEFAULT '',
    city VARCHAR(255) NOT NULL DEFAULT '',
    country CHAR(2) NOT NULL DEFAULT '',

    email VARCHAR(255) NOT NULL,
    email_alternative VARCHAR(255) NOT NULL DEFAULT '',
    matrix VARCHAR(255) NOT NULL DEFAULT '',
    mastodon VARCHAR(255) NOT NULL DEFAULT '',
    linkedin VARCHAR(255) NOT NULL DEFAULT '',

    sepa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    sepa_account_holder VARCHAR(255) NOT NULL DEFAULT '',
    sepa_mandate_reference VARCHAR(35) NOT NULL DEFAULT '',
    sepa_iban VARCHAR(34) NOT NULL DEFAULT '',
    sepa_bic VARCHAR(11) NOT NULL DEFAULT '',

    jug VARCHAR(255) NOT NULL DEFAULT '',
    newsletter BOOLEAN NOT NULL DEFAULT FALSE,

    PRIMARY KEY (id),
    CONSTRAINT fk_clubdesk_user
        FOREIGN KEY (id)
            REFERENCES user (id)
);
