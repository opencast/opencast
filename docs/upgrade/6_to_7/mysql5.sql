ALTER TABLE oc_assets_asset ADD CONSTRAINT FK_oc_assets_asset_snapshot_id FOREIGN KEY (snapshot_id) REFERENCES oc_assets_snapshot (id) ON DELETE CASCADE; 
