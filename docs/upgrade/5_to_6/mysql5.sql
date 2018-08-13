ALTER TABLE oc_assets_snapshot ADD COLUMN storage_id VARCHAR(256) NOT NULL DEFAULT 'local-filesystem';
ALTER TABLE oc_assets_asset ADD COLUMN storage_id VARCHAR(256) NOT NULL DEFAULT 'local-filesystem';

