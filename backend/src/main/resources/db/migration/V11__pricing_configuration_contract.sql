-- B3配置契约：只扩展容量和配置版本，不重写既有报价/规则快照。
ALTER TABLE service_item
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    MODIFY name VARCHAR(200) NOT NULL,
    MODIFY description VARCHAR(1000) NULL;
ALTER TABLE pricing_rule
    MODIFY rule_version VARCHAR(64) NOT NULL,
    MODIFY title VARCHAR(200) NOT NULL,
    MODIFY description VARCHAR(2000) NULL,
    MODIFY unit VARCHAR(32) NOT NULL,
    MODIFY refund_policy VARCHAR(1000) NULL;
