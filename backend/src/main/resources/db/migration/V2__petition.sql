CREATE TABLE case_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(1024),
    template_key VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_case_categories_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    details LONGTEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cases_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_cases_category FOREIGN KEY (category_id) REFERENCES case_categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_cases_customer ON cases (customer_id, updated_at);

-- Append-only: regeneration inserts a new row with an incremented version, never an UPDATE.
CREATE TABLE petitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT NOT NULL,
    generated_content LONGTEXT NOT NULL,
    pdf_url VARCHAR(512) NOT NULL,
    docx_url VARCHAR(512) NOT NULL,
    version INT NOT NULL,
    disclaimer_version VARCHAR(16) NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_petitions_case FOREIGN KEY (case_id) REFERENCES cases (id),
    CONSTRAINT uq_petitions_case_version UNIQUE (case_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_petitions_case ON petitions (case_id, version);

INSERT INTO case_categories (slug, name, description, template_key, active) VALUES
    ('consumer-complaint', 'Consumer Complaint', 'File a complaint against a business for defective goods or deficient services.', 'consumer-complaint', TRUE),
    ('rent-dispute', 'Rent / Tenancy Dispute', 'Disputes between landlords and tenants over rent, eviction, or deposits.', 'rent-dispute', TRUE),
    ('cheque-bounce', 'Cheque Bounce (Section 138)', 'Legal notice and complaint for a dishonoured cheque under the Negotiable Instruments Act.', 'cheque-bounce', TRUE),
    ('employment-dispute', 'Employment Dispute', 'Wrongful termination, unpaid wages, or other employer-employee disputes.', 'employment-dispute', TRUE);
