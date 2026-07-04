CREATE TABLE chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    counterpart_type VARCHAR(16) NOT NULL,
    lawyer_id BIGINT,
    started_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ended_at TIMESTAMP(6) NULL,
    last_metered_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    billed_seconds BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_chat_sessions_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_chat_sessions_lawyer FOREIGN KEY (lawyer_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_chat_sessions_customer_status ON chat_sessions (customer_id, status);
CREATE INDEX idx_chat_sessions_status ON chat_sessions (status);

-- Append-only (CLAUDE.md domain rule #5). No UPDATE/DELETE is ever issued against this table.
CREATE TABLE chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    sender VARCHAR(16) NOT NULL,
    content LONGTEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id) REFERENCES chat_sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_chat_messages_session ON chat_messages (session_id, timestamp);
