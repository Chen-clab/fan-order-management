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
