-- fan commerce schema: MySQL 8, install after the upstream system schema.
-- Currency is USD in v1; amounts use fixed-point values.
CREATE TABLE fan_merchant (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  credit_code VARCHAR(64) NOT NULL,
  contact_name VARCHAR(100) NOT NULL,
  contact_phone VARCHAR(40) NOT NULL,
  email VARCHAR(200),
  qualification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_merchant_credit_code (credit_code),
  KEY idx_merchant_status (qualification_status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fan_merchant_user (
  merchant_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  PRIMARY KEY (merchant_id, user_id),
  UNIQUE KEY uk_merchant_user (user_id),
  CONSTRAINT fk_merchant_user_merchant FOREIGN KEY (merchant_id) REFERENCES fan_merchant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fan_product (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  name VARCHAR(200) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_product_merchant_id (merchant_id, id),
  KEY idx_product_merchant (merchant_id, status, id),
  CONSTRAINT fk_product_merchant FOREIGN KEY (merchant_id) REFERENCES fan_merchant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fan_product_link (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  url VARCHAR(2048) NOT NULL,
  platform VARCHAR(80) NOT NULL,
  UNIQUE KEY uk_product_link (product_id, id),
  CONSTRAINT fk_link_product FOREIGN KEY (product_id) REFERENCES fan_product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fan_cooperation_order (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(64) NOT NULL,
  merchant_id BIGINT NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  agreed_publish_count INT UNSIGNED NOT NULL,
  quoted_fee DECIMAL(18,2) NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'USD',
  version BIGINT NOT NULL DEFAULT 0,
  confirmed_version BIGINT,
  confirmed_by BIGINT,
  confirmed_at DATETIME(3),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_order_no (order_no),
  UNIQUE KEY uk_order_merchant_id (merchant_id,id),
  KEY idx_order_merchant_status (merchant_id,status,id),
  CONSTRAINT ck_order_fee CHECK (quoted_fee >= 0),
  CONSTRAINT ck_order_currency CHECK (currency = 'USD'),
  CONSTRAINT fk_order_merchant FOREIGN KEY (merchant_id) REFERENCES fan_merchant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fan_order_item (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name_snapshot VARCHAR(200) NOT NULL,
  commission_rate DECIMAL(9,6) NOT NULL,
  UNIQUE KEY uk_order_product (order_id,product_id),
  UNIQUE KEY uk_item_product (id,product_id),
  CONSTRAINT ck_item_rate CHECK (commission_rate >= 0 AND commission_rate <= 1),
  CONSTRAINT fk_item_order FOREIGN KEY (merchant_id,order_id) REFERENCES fan_cooperation_order(merchant_id,id),
  CONSTRAINT fk_item_product FOREIGN KEY (merchant_id,product_id) REFERENCES fan_product(merchant_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fan_video (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_item_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_link_id BIGINT NOT NULL,
  link_snapshot VARCHAR(2048) NOT NULL,
  external_video_id VARCHAR(160) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_video_external (external_video_id),
  KEY idx_video_item (order_item_id,id),
  CONSTRAINT fk_video_item FOREIGN KEY (order_item_id,product_id) REFERENCES fan_order_item(id,product_id),
  CONSTRAINT fk_video_link FOREIGN KEY (product_id,product_link_id) REFERENCES fan_product_link(product_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fan_video_publish (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  video_id BIGINT NOT NULL,
  platform VARCHAR(80) NOT NULL,
  external_publish_id VARCHAR(160) NOT NULL,
  publish_url VARCHAR(2048) NOT NULL,
  published_at DATETIME(3) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  UNIQUE KEY uk_publish_external (platform,external_publish_id),
  KEY idx_publish_video (video_id,status,id),
  CONSTRAINT fk_publish_video FOREIGN KEY (video_id) REFERENCES fan_video(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Qualification review history, retained after a decision.
CREATE TABLE fan_merchant_review (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  reviewer_id BIGINT NOT NULL,
  decision VARCHAR(20) NOT NULL,
  reason VARCHAR(1000),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_review_merchant (merchant_id,id),
  CONSTRAINT fk_review_merchant FOREIGN KEY (merchant_id) REFERENCES fan_merchant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Source-reported video GMV is already net of refunds. Each snapshot has one row per source/video/as-of/version.
CREATE TABLE fan_video_gmv_snapshot (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  video_id BIGINT NOT NULL,
  source VARCHAR(80) NOT NULL,
  snapshot_at DATETIME NOT NULL,
  source_version BIGINT NOT NULL DEFAULT 1,
  currency CHAR(3) NOT NULL DEFAULT 'USD',
  final_gmv DECIMAL(18,2) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_video_gmv_snapshot (source,video_id,snapshot_at,source_version),
  KEY idx_gmv_latest (video_id,source,snapshot_at,source_version),
  CONSTRAINT ck_gmv_nonnegative CHECK (final_gmv >= 0),
  CONSTRAINT ck_gmv_currency CHECK (currency = 'USD'),
  CONSTRAINT fk_gmv_video FOREIGN KEY (video_id) REFERENCES fan_video(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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

CREATE TABLE fan_import_batch (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 source VARCHAR(80) NOT NULL,
 snapshot_at DATETIME NOT NULL,
 source_version BIGINT NOT NULL,
 file_sha256 CHAR(64) NOT NULL,
 imported_by BIGINT NOT NULL,
 inserted_count INT UNSIGNED NOT NULL,
 skipped_count INT UNSIGNED NOT NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 KEY idx_import_source_time (source,snapshot_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fan_async_import (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 source VARCHAR(80) NOT NULL,
 snapshot_at DATETIME NOT NULL,
 source_version BIGINT NOT NULL,
 requested_by BIGINT NOT NULL,
 file_name VARCHAR(255) NOT NULL,
 file_data MEDIUMBLOB NOT NULL,
 file_sha256 CHAR(64) NOT NULL,
 state VARCHAR(32) NOT NULL,
 total_rows INT UNSIGNED NOT NULL DEFAULT 0,
 preview_json MEDIUMTEXT NULL,
 result_json TEXT NULL,
 error_message VARCHAR(1000) NULL,
 version BIGINT NOT NULL DEFAULT 0,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 KEY idx_async_owner (requested_by,id),
 KEY idx_async_state (state,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE fan_import_outbox (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 job_id BIGINT NOT NULL,
 event_type VARCHAR(24) NOT NULL,
 state VARCHAR(20) NOT NULL DEFAULT 'NEW',
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 sent_at DATETIME(3),
 UNIQUE KEY uk_import_event (job_id,event_type),
 KEY idx_outbox_pending (state,id),
 CONSTRAINT fk_outbox_job FOREIGN KEY (job_id) REFERENCES fan_async_import(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE fan_merchant_document MODIFY content MEDIUMBLOB NULL, ADD COLUMN object_key VARCHAR(255) NULL;
ALTER TABLE fan_product_asset MODIFY content MEDIUMBLOB NULL, ADD COLUMN object_key VARCHAR(255) NULL;

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

