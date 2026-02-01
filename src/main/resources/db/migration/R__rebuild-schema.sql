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
    
    firstname VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    address_addition VARCHAR(255) DEFAULT NULL,
    zip_code VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,

    email VARCHAR(255) NOT NULL,
    email_alternative VARCHAR(255) DEFAULT NULL,
    mastodon VARCHAR(255) DEFAULT NULL,
    matrix VARCHAR(255) DEFAULT NULL,
    linkedin VARCHAR(255) DEFAULT NULL,

    sepa BOOLEAN NOT NULL DEFAULT FALSE,
    sepa_mandate_reference VARCHAR(255) DEFAULT NULL,
    sepa_mandate_date TIMESTAMP DEFAULT NULL,
    sepa_type VARCHAR(50) DEFAULT NULL,
    sepa_last_debit_date TIMESTAMP DEFAULT NULL,
    sepa_account_holder VARCHAR(255) DEFAULT NULL,
    sepa_iban VARCHAR(34) DEFAULT NULL,
    sepa_bic VARCHAR(11) DEFAULT NULL,

    newsletter BOOLEAN NOT NULL DEFAULT FALSE,
    jug VARCHAR(255) DEFAULT NULL,

    CHECK (country IN ('Deutschland', 'Österreich', 'Schweiz')),
    PRIMARY KEY (id),
    CONSTRAINT fk_clubdesk_user
        FOREIGN KEY (id)
            REFERENCES user (id)
);
