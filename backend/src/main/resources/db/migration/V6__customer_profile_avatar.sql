-- CHG-0007 / ADR-0007：仅新增头像文件关联，不改昵称等已有资料列。
ALTER TABLE customer_user
    ADD COLUMN avatar_file_id BIGINT NULL,
    ADD KEY idx_customer_avatar (avatar_file_id),
    ADD CONSTRAINT fk_customer_avatar FOREIGN KEY (avatar_file_id) REFERENCES file_asset(id);
