CREATE TABLE tuition_packages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    sessions_per_week INT NULL,
    total_sessions INT NOT NULL,
    expected_months INT NULL,
    price DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_tuition_packages_name ON tuition_packages (name);
CREATE INDEX idx_tuition_packages_status ON tuition_packages (status);
