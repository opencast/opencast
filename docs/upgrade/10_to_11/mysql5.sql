-- Increase mime_type field size
ALTER TABLE oc_assets_asset MODIFY COLUMN mime_type VARCHAR (255);