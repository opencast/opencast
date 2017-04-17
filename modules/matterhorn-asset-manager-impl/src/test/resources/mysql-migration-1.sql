# migration-1 contains pre data migration steps
SET FOREIGN_KEY_CHECKS = 0;

####
# mh_archive_episode -> mh_assets_snapshot
##

# drop indices and constraints
DROP INDEX IX_mh_archive_episode_mediapackage ON mh_archive_episode;
DROP INDEX IX_mh_archive_episode_version ON mh_archive_episode;
DROP INDEX IX_mh_archive_episode_deleted ON mh_archive_episode;
DROP INDEX FK_mh_archive_episode_organization ON mh_archive_episode;
ALTER TABLE mh_archive_episode DROP FOREIGN KEY mh_archive_episode_organization;

# rename table; rename/add/drop columns
ALTER TABLE mh_archive_episode
RENAME TO mh_assets_snapshot,
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

UPDATE mh_assets_snapshot SET availability = 'ONLINE';


####
# mh_archive_asset -> mh_assets_asset
##

# drop indices and constraints
DROP INDEX UNQ_mh_archive_asset_0 ON mh_archive_asset;
DROP INDEX IX_mh_archive_asset_mediapackage ON mh_archive_asset;
DROP INDEX IX_mh_archive_asset_checksum ON mh_archive_asset;
DROP INDEX IX_mh_archive_asset_uri ON mh_archive_asset;
ALTER TABLE mh_archive_asset DROP FOREIGN KEY mh_archive_asset_organization;

# rename table; rename/add/drop columns
ALTER TABLE mh_archive_asset
RENAME TO mh_assets_asset,
CHANGE COLUMN mediapackageelement mediapackage_element_id VARCHAR(128) NOT NULL,
MODIFY COLUMN checksum VARCHAR(64) NOT NULL,
ADD COLUMN snapshot_id BIGINT(20) NOT NULL,
ADD COLUMN mime_type VARCHAR(64),
ADD COLUMN size BIGINT(20) NOT NULL,
DROP COLUMN organization,
DROP COLUMN uri;


####
# mh_archive_version_claim -> mh_assets_version_claim
##

# drop indices and constraints
DROP INDEX IX_mh_archive_version_claim_mediapackage ON mh_archive_version_claim;
DROP INDEX IX_mh_archive_version_claim_last_claimed ON mh_archive_version_claim;

# rename table; rename/add/drop columns
ALTER TABLE mh_archive_version_claim
RENAME TO mh_assets_version_claim,
CHANGE COLUMN mediapackage mediapackage_id VARCHAR(128) NOT NULL,
MODIFY COLUMN last_claimed BIGINT(20) NOT NULL;

####
# mh_assets_properties
##

# create table
CREATE TABLE `mh_assets_properties` (
  `id`              BIGINT(20)   NOT NULL,
  `val_bool`        TINYINT(1)   DEFAULT '0',
  `val_date`        DATETIME     DEFAULT NULL,
  `val_long`        BIGINT(20)   DEFAULT NULL,
  `val_string`      VARCHAR(255) DEFAULT NULL,
  `mediapackage_id` VARCHAR(128) NOT NULL,
  `namespace`       VARCHAR(128) NOT NULL,
  `property_name`   VARCHAR(128) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `IX_mh_assets_properties_val_date` (`val_date`),
  KEY `IX_mh_assets_properties_val_long` (`val_long`),
  KEY `IX_mh_assets_properties_val_string` (`val_string`),
  KEY `IX_mh_assets_properties_val_bool` (`val_bool`),
  KEY `IX_mh_assets_properties_mediapackage_id` (`mediapackage_id`),
  KEY `IX_mh_assets_properties_namespace` (`namespace`),
  KEY `IX_mh_assets_properties_property_name` (`property_name`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

SET FOREIGN_KEY_CHECKS = 1;
