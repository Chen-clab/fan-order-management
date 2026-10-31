
-- Versioned proposed actual service fee; no funds move through this system.
CREATE TABLE fan_fee_adjustment (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 order_id BIGINT NOT NULL,
 actual_fee DECIMAL(18,2) NOT NULL,
 reason VARCHAR(1000) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 created_by BIGINT NOT NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 decided_by BIGINT,
 decided_at DATETIME(3),
 decision_note VARCHAR(1000),
 version BIGINT NOT NULL DEFAULT 0,
 KEY idx_fee_order (order_id,id),
 CONSTRAINT ck_fee_nonnegative CHECK (actual_fee>=0),
 CONSTRAINT fk_fee_order FOREIGN KEY (order_id) REFERENCES fan_cooperation_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
