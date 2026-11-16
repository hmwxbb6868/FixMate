-- ADR-0005/0006：原工单状态不扩展；历史人员镜像不伪造已接受分派。
ALTER TABLE safety_event
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN acknowledgement_note VARCHAR(1000) NULL,
    ADD COLUMN resolution_basis VARCHAR(1000) NULL;

CREATE TABLE work_order_assignment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    work_order_id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL,
    assigned_by BIGINT NOT NULL,
    assigned_at DATETIME(3) NOT NULL,
    accepted_at DATETIME(3) NULL,
    ended_by BIGINT NULL,
    ended_at DATETIME(3) NULL,
    end_reason VARCHAR(1000) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    occupied_work_order_id BIGINT GENERATED ALWAYS AS
      (CASE WHEN status IN ('PENDING','ACTIVE','RETURN_REQUESTED') THEN work_order_id ELSE NULL END) STORED,
    UNIQUE KEY uk_assignment_current(occupied_work_order_id),
    KEY idx_assignment_technician(technician_id,status,assigned_at),
    KEY idx_assignment_work_order(work_order_id,id),
    CONSTRAINT fk_assignment_order FOREIGN KEY(work_order_id) REFERENCES work_order(id),
    CONSTRAINT fk_assignment_technician FOREIGN KEY(technician_id) REFERENCES staff_user(id),
    CONSTRAINT fk_assignment_dispatcher FOREIGN KEY(assigned_by) REFERENCES staff_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE work_order
    ADD COLUMN current_assignment_id BIGINT NULL,
    ADD COLUMN takeover_by BIGINT NULL,
    ADD COLUMN takeover_at DATETIME(3) NULL,
    ADD COLUMN takeover_reason VARCHAR(1000) NULL,
    ADD CONSTRAINT fk_order_current_assignment FOREIGN KEY(current_assignment_id) REFERENCES work_order_assignment(id);
