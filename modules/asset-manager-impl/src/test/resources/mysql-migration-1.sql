# migration-1 contains pre data migration steps
SET FOREIGN_KEY_CHECKS = 0;

####
# oc_archive_episode -> oc_assets_snapshot
##

# drop indices and constraints
DROP INDEX IX_oc_archive_episode_mediapackage ON oc_archive_episode;
DROP INDEX IX_oc_archive_episode_version ON oc_archive_episode;
DROP INDEX IX_oc_archive_episode_deleted ON oc_archive_episode;
DROP INDEX FK_oc_archive_episode_organization ON oc_archive_episode;
ALTER TABLE oc_archive_episode DROP FOREIGN KEY oc_archive_episode_organization;

# rename table; rename/add/drop columns
ALTER TABLE oc_archive_episode
RENAME TO oc_assets_snapshot,
DROP PRIMARY KEY,
CHANGE COLUMN id mediapackage_id VARCHAR(128) NOT NULL,
CHANGE COLUMN organization organization_id VARCHAR(128) NOT NULL,
CHANGE COLUMN modification_date archival_date DATETIME NOT NULL,
MODIFY COLUMN mediapackage_xml LONGTEXT NOT NULL,
MODIFY COLUMN version BIGINT(20) NOT NULL,
ADD COLUMN id BIGINT(20) NOT NULL,
ADD COLUMN availability VARCHAR(32) NOT NULL,
ADD COLUMN series_id VARCHAR(128),
DROP COLUMN access_control;

UPDATE oc_assets_snapshot SET availability = 'ONLINE';


####
# oc_archive_asset -> oc_assets_asset
##

# drop indices and constraints
DROP INDEX UNQ_oc_archive_asset_0 ON oc_archive_asset;
DROP INDEX IX_oc_archive_asset_mediapackage ON oc_archive_asset;
DROP INDEX IX_oc_archive_asset_checksum ON oc_archive_asset;
DROP INDEX IX_oc_archive_asset_uri ON oc_archive_asset;
ALTER TABLE oc_archive_asset DROP FOREIGN KEY oc_archive_asset_organization;

# rename table; rename/add/drop columns
ALTER TABLE oc_archive_asset
RENAME TO oc_assets_asset,
CHANGE COLUMN mediapackageelement mediapackage_element_id VARCHAR(128) NOT NULL,
MODIFY COLUMN checksum VARCHAR(64) NOT NULL,
ADD COLUMN snapshot_id BIGINT(20) NOT NULL,
ADD COLUMN mime_type VARCHAR(64),
ADD COLUMN size BIGINT(20) NOT NULL,
DROP COLUMN organization,
DROP COLUMN uri;


####
# oc_archive_version_claim -> oc_assets_version_claim
##

# drop indices and constraints
DROP INDEX IX_oc_archive_version_claim_mediapackage ON oc_archive_version_claim;
DROP INDEX IX_oc_archive_version_claim_last_claimed ON oc_archive_version_claim;

# rename table; rename/add/drop columns
ALTER TABLE oc_archive_version_claim
RENAME TO oc_assets_version_claim,
CHANGE COLUMN mediapackage mediapackage_id VARCHAR(128) NOT NULL,
MODIFY COLUMN last_claimed BIGINT(20) NOT NULL;

####
# oc_assets_properties
##

# create table
CREATE TABLE `oc_assets_properties` (
  `id`              BIGINT(20)   NOT NULL,
  `val_bool`        TINYINT(1)   DEFAULT '0',
  `val_date`        DATETIME     DEFAULT NULL,
  `val_long`        BIGINT(20)   DEFAULT NULL,
  `val_string`      VARCHAR(255) DEFAULT NULL,
  `mediapackage_id` VARCHAR(128) NOT NULL,
  `namespace`       VARCHAR(128) NOT NULL,
  `property_name`   VARCHAR(128) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IX_oc_assets_properties_val_date` (`val_date`),
  KEY `IX_oc_assets_properties_val_long` (`val_long`),
  KEY `IX_oc_assets_properties_val_string` (`val_string`),
  KEY `IX_oc_assets_properties_val_bool` (`val_bool`),
  KEY `IX_oc_assets_properties_mediapackage_id` (`mediapackage_id`),
  KEY `IX_oc_assets_properties_namespace` (`namespace`),
  KEY `IX_oc_assets_properties_property_name` (`property_name`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

SET FOREIGN_KEY_CHECKS = 1;
