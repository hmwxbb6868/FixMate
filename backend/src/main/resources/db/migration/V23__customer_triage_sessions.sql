CREATE TABLE triage_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    repair_request_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    conversation JSON NOT NULL,
    submitted_summary TEXT NULL,
    confirmed_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_triage_repair (repair_request_id),
    CONSTRAINT fk_triage_repair FOREIGN KEY (repair_request_id) REFERENCES repair_request(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
