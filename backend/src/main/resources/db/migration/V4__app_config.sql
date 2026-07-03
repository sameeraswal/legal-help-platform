-- Single-row table (id is always 1); see AppConfig / AppConfigService.
CREATE TABLE app_config (
    id BIGINT PRIMARY KEY,
    free_minutes INT NOT NULL,
    payout_threshold_minor_units BIGINT NOT NULL,
    pg_key_id_encrypted TEXT,
    pg_key_secret_encrypted TEXT,
    pg_webhook_secret_encrypted TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
