# migration-3 contains post data migration steps
SET FOREIGN_KEY_CHECKS = 0;

####
# mh_assets_snapshot
##

ALTER TABLE mh_assets_snapshot
DROP COLUMN deleted;

# add indices
ALTER TABLE mh_assets_snapshot
ADD PRIMARY KEY (id),
ADD INDEX IX_mh_assets_snapshot_archival_date (archival_date),
ADD INDEX IX_mh_assets_snapshot_mediapackage_id (mediapackage_id),
ADD INDEX IX_mh_assets_snapshot_organization_id (organization_id),
ADD CONSTRAINT UNQ_mh_assets_snapshot UNIQUE (mediapackage_id, version);
ALTER TABLE mh_assets_snapshot
ADD CONSTRAINT FK_mh_assets_snapshot_organization FOREIGN KEY (organization_id) REFERENCES mh_organization (id);


####
# mh_assets_asset
##

ALTER TABLE mh_assets_asset
DROP COLUMN version,
DROP COLUMN mediapackage;

# add indices
ALTER TABLE mh_assets_asset
ADD INDEX IX_mh_assets_asset_checksum (checksum),
ADD INDEX IX_mh_assets_asset_mediapackage_element_id (mediapackage_element_id);


####
# mh_assets_version_claim
##

SET FOREIGN_KEY_CHECKS = 1;
