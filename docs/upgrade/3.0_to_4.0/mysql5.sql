#######################################################################################
# Migrate archive to asset manager, schema and data.                                  #
# Migrate OAI-PMH, schema and data.                                                   #
#######################################################################################

SET FOREIGN_KEY_CHECKS = 0;


#######################################################################################
# Schema migration                                                                    #
#######################################################################################

####
# mh_archive_episode -> mh_assets_snapshot
##

# drop indices and constraints
DROP INDEX IX_mh_archive_episode_version ON mh_archive_episode;
DROP INDEX IX_mh_archive_episode_deleted ON mh_archive_episode;
ALTER TABLE mh_archive_episode DROP FOREIGN KEY FK_mh_archive_episode_organization;
DROP INDEX IX_mh_archive_episode_organization ON mh_archive_episode;

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
ADD COLUMN owner VARCHAR(256) NOT NULL,
DROP COLUMN access_control;

UPDATE mh_assets_snapshot SET availability = 'ONLINE';


####
# mh_archive_asset -> mh_assets_asset
##

# drop indices and constraints
DROP INDEX IX_mh_archive_asset_mediapackage ON mh_archive_asset;
DROP INDEX IX_mh_archive_asset_checksum ON mh_archive_asset;
DROP INDEX IX_mh_archive_asset_uri ON mh_archive_asset;
ALTER TABLE mh_archive_asset DROP FOREIGN KEY FK_mh_archive_asset_organization;
DROP INDEX UNQ_mh_archive_asset ON mh_archive_asset;

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

####
# mh_oaipmh
##

ALTER TABLE `mh_oaipmh`
CHANGE COLUMN `id` `mp_id` VARCHAR(128) NOT NULL;

####
# mh_oaipmh_elements
##

CREATE TABLE `mh_oaipmh_elements` (
  `id` INT(20) NOT NULL AUTO_INCREMENT,
  `element_type` VARCHAR(16) NOT NULL,
  `flavor` varchar(255) NOT NULL,
  `xml` TEXT(65535) NOT NULL,
  `mp_id` VARCHAR(128) NOT NULL,
  `organization` VARCHAR(128) NOT NULL,
  `repo_id` VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `FK_mh_oaipmh_elements`
    FOREIGN KEY (`mp_id`, `repo_id`, `organization`)
    REFERENCES `mh_oaipmh` (`mp_id`, `repo_id`, `organization`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


#######################################################################################
# Data migration                                                                      #
#######################################################################################

####
# mh_assets_snapshot
##

# create IDs (PK); the archive table did not use surrogate IDs
SET @mh_assets_snapshot_id := 0;
UPDATE mh_assets_snapshot
SET id = @mh_assets_snapshot_id := @mh_assets_snapshot_id + 1;

# Now update the sequence generator for mh_assets_snapshot to a value
# greater than the last ID generated by the above statement.
# Otherwise duplicate key exceptions are most likely to happen when new episodes
# are being inserted by the AssetManager.
UPDATE SEQUENCE
SET SEQ_COUNT = @mh_assets_snapshot_id
WHERE SEQ_NAME = 'seq_mh_assets_snapshot';

# set series
UPDATE mh_assets_snapshot a
SET a.series_id = (SELECT substring(a.mediapackage_xml,
                                    locate('<series>', a.mediapackage_xml) + 8,
                                    locate('</series>', a.mediapackage_xml)
                                    - locate('<series>', a.mediapackage_xml) - 8));

####
# mh_assets_asset
##

# link with mh_assets_snapshot: create foreign keys
UPDATE mh_assets_asset a
SET a.snapshot_id = (SELECT id
                     FROM mh_assets_snapshot
                     WHERE a.mediapackage = mediapackage_id AND a.version = version);

# rewrite IDs starting with 1
SET @mh_assets_asset_id = (SELECT min(id) - 1
                           FROM mh_assets_asset);
UPDATE mh_assets_asset a
SET a.id = a.id - @mh_assets_asset_id;

# update the sequence generator
INSERT INTO SEQUENCE
(SEQ_COUNT, SEQ_NAME)
VALUES ((SELECT max(id) + 1
         FROM mh_assets_asset), 'seq_mh_assets_asset');
INSERT INTO SEQUENCE
(SEQ_COUNT, SEQ_NAME)
VALUES ((SELECT max(id) + 1
         FROM mh_assets_snapshot), 'seq_mh_assets_snapshot');

####
# mh_oaipmh_elements data migration
##

# move episode dublincore from mh_oaipmh.episode_dublincore_xml to mh_oaipmh_elements.xml
INSERT INTO mh_oaipmh_elements (element_type, flavor, xml, mp_id, organization, repo_id)
SELECT 'Catalog', 'dublincore/episode', o.episode_dublincore_xml, o.mp_id, o.organization, o.repo_id
FROM mh_oaipmh AS o
WHERE o.episode_dublincore_xml IS NOT NULL;

# move series dublincore from mh_oaipmh.series_dublincore_xml to mh_oaipmh_elements.xml
INSERT INTO mh_oaipmh_elements (element_type, flavor, xml, mp_id, organization, repo_id)
SELECT 'Catalog', 'dublincore/series', o.series_dublincore_xml, o.mp_id, o.organization, o.repo_id
FROM mh_oaipmh AS o
WHERE o.series_dublincore_xml IS NOT NULL;

# move series acl from mh_oaipmh.series_acl_xml to mh_oaipmh_elements.xml
INSERT INTO mh_oaipmh_elements (element_type, flavor, xml, mp_id, organization, repo_id)
SELECT 'Attachment', 'security/xacml+series', o.series_acl_xml, o.mp_id, o.organization, o.repo_id
FROM mh_oaipmh AS o
WHERE o.series_acl_xml IS NOT NULL;

#######################################################################################
# Data migration post processing                                                      #
#######################################################################################

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
ADD INDEX IX_mh_assets_snapshot_owner (owner),
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
# mh_oaipmh
##

ALTER TABLE mh_oaipmh
DROP COLUMN episode_dublincore_xml,
DROP COLUMN series_dublincore_xml,
DROP COLUMN series_acl_xml;


SET FOREIGN_KEY_CHECKS = 1;

###
# Create table for managing watson jobs.
###
CREATE TABLE mh_ibm_watson_transcript_job (
    id BIGINT(20) NOT NULL,
    media_package_id VARCHAR(128) NOT NULL,
    track_id VARCHAR(128) NOT NULL,
    job_id  VARCHAR(128) NOT NULL,
    date_created datetime NOT NULL,
    date_completed datetime DEFAULT NULL,
    status VARCHAR(128) DEFAULT NULL,
    track_duration BIGINT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE mh_scheduled_last_modified (
  capture_agent_id VARCHAR(255) NOT NULL,
  last_modified DATETIME NOT NULL,
  PRIMARY KEY (capture_agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX IX_mh_scheduled_last_modified_last_modified ON mh_scheduled_last_modified (last_modified);

CREATE TABLE mh_scheduled_extended_event (
  mediapackage_id VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  PRIMARY KEY (mediapackage_id, organization),
  CONSTRAINT FK_mh_scheduled_extended_event_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE mh_scheduled_transaction (
  id VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  source VARCHAR(255) NOT NULL,
  last_modified DATETIME NOT NULL,
  PRIMARY KEY (id, organization),
  CONSTRAINT UNQ_mh_scheduled_transaction UNIQUE (id, organization, source),
  CONSTRAINT FK_mh_scheduled_transaction_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX IX_mh_scheduled_transaction_source ON mh_scheduled_transaction (source);
