CREATE TABLE fan_merchant_document (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 merchant_id BIGINT NOT NULL,
 media_type VARCHAR(40) NOT NULL,
 content MEDIUMBLOB NOT NULL,
 uploaded_by BIGINT NOT NULL,
 uploaded_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 KEY idx_document_merchant (merchant_id,id),
 CONSTRAINT fk_document_merchant FOREIGN KEY (merchant_id) REFERENCES fan_merchant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
