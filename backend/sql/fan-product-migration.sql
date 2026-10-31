ALTER TABLE fan_product ADD COLUMN description TEXT NULL, ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE fan_product_asset (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 product_id BIGINT NOT NULL,
 content MEDIUMBLOB NOT NULL,
 media_type VARCHAR(50) NOT NULL,
 uploaded_by BIGINT NOT NULL,
 uploaded_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 KEY idx_asset_product (product_id,id),
 CONSTRAINT fk_asset_product FOREIGN KEY (product_id) REFERENCES fan_product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE fan_product_review (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 product_id BIGINT NOT NULL,
 reviewer_id BIGINT NOT NULL,
 decision VARCHAR(20) NOT NULL,
 reason VARCHAR(1000),
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 KEY idx_product_review (product_id,id),
 CONSTRAINT fk_product_review FOREIGN KEY (product_id) REFERENCES fan_product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
