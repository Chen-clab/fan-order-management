CREATE TABLE fan_settlement (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 order_id BIGINT NOT NULL,
 source VARCHAR(80) NOT NULL,
 snapshot_at DATETIME NOT NULL,
 currency CHAR(3) NOT NULL DEFAULT 'USD',
 final_gmv DECIMAL(18,2) NOT NULL,
 commission DECIMAL(18,2) NOT NULL,
 service_fee DECIMAL(18,2) NOT NULL,
 receivable DECIMAL(18,2) NOT NULL,
 received DECIMAL(18,2) NOT NULL DEFAULT 0,
 state VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
 version BIGINT NOT NULL DEFAULT 0,
 created_by BIGINT NOT NULL,
 approved_by BIGINT NULL,
 approved_at DATETIME(3) NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 UNIQUE KEY uk_settlement_order (order_id),
 CONSTRAINT fk_settlement_order FOREIGN KEY (order_id) REFERENCES fan_cooperation_order(id),
 CONSTRAINT ck_settlement_amounts CHECK (final_gmv>=0 AND commission>=0 AND service_fee>=0 AND receivable>=0 AND received>=0 AND received<=receivable),
 CONSTRAINT ck_settlement_currency CHECK (currency='USD')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE fan_settlement_receipt (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 settlement_id BIGINT NOT NULL,
 reference_no VARCHAR(100) NOT NULL,
 amount DECIMAL(18,2) NOT NULL,
 note VARCHAR(1000) NULL,
 recorded_by BIGINT NOT NULL,
 recorded_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 UNIQUE KEY uk_settlement_reference (settlement_id,reference_no),
 CONSTRAINT ck_receipt_amount CHECK (amount>0),
 CONSTRAINT fk_receipt_settlement FOREIGN KEY (settlement_id) REFERENCES fan_settlement(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
