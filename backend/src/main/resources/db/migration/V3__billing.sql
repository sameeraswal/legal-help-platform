CREATE TABLE plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    price_minor_units BIGINT NOT NULL,
    seconds INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customer_wallets (
    customer_id BIGINT PRIMARY KEY,
    remaining_seconds BIGINT NOT NULL DEFAULT 0,
    free_seconds_remaining BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_customer_wallets_user FOREIGN KEY (customer_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lawyer_wallets (
    lawyer_id BIGINT PRIMARY KEY,
    balance_minor_units BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_lawyer_wallets_user FOREIGN KEY (lawyer_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Append-only, double-entry-style journal. No UPDATE/DELETE is ever issued against this table.
CREATE TABLE wallet_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_type VARCHAR(16) NOT NULL,
    entry_type VARCHAR(16) NOT NULL,
    seconds_delta BIGINT,
    amount_delta_minor_units BIGINT,
    balance_after_seconds BIGINT,
    balance_after_amount_minor_units BIGINT,
    reference VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_ledger_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_wallet_ledger_user ON wallet_ledger (user_id, created_at);

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(128) NOT NULL,
    pg_order_id VARCHAR(128),
    pg_ref VARCHAR(128),
    customer_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    amount_minor_units BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    raw_webhook_payload LONGTEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_payments_order_id UNIQUE (order_id),
    CONSTRAINT uq_payments_pg_order_id UNIQUE (pg_order_id),
    CONSTRAINT uq_payments_pg_ref UNIQUE (pg_ref),
    CONSTRAINT fk_payments_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_payments_plan FOREIGN KEY (plan_id) REFERENCES plans (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_payments_status ON payments (status, created_at);
CREATE INDEX idx_payments_customer ON payments (customer_id, created_at);

CREATE TABLE refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    amount_minor_units BIGINT NOT NULL,
    pg_refund_ref VARCHAR(128),
    admin_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'INITIATED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id),
    CONSTRAINT fk_refunds_admin FOREIGN KEY (admin_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- New rows only — a rate change is a new row with a later effective_from, never an edit.
CREATE TABLE lawyer_rates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lawyer_id BIGINT,
    per_minute_rate_minor_units BIGINT NOT NULL,
    effective_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lawyer_rates_lawyer FOREIGN KEY (lawyer_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_lawyer_rates_lawyer ON lawyer_rates (lawyer_id, effective_from);

CREATE TABLE payout_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lawyer_id BIGINT NOT NULL,
    amount_minor_units BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    admin_id BIGINT,
    bank_reference VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP NULL,
    CONSTRAINT fk_payout_requests_lawyer FOREIGN KEY (lawyer_id) REFERENCES users (id),
    CONSTRAINT fk_payout_requests_admin FOREIGN KEY (admin_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_payout_requests_status ON payout_requests (status, created_at);

INSERT INTO plans (name, price_minor_units, seconds, active) VALUES
    ('Quick Chat — ₹10 / 5 min', 1000, 300, TRUE),
    ('Standard — ₹20 / 15 min', 2000, 900, TRUE),
    ('Extended — ₹100 / 90 min', 10000, 5400, TRUE);

INSERT INTO lawyer_rates (lawyer_id, per_minute_rate_minor_units) VALUES (NULL, 500);
