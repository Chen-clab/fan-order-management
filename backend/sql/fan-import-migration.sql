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
