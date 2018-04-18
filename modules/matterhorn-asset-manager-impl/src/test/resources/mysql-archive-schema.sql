# setup the archive tables

CREATE TABLE mh_archive_asset
(
    id BIGINT PRIMARY KEY NOT NULL,
    mediapackageelement VARCHAR(128) NOT NULL,
    mediapackage VARCHAR(128) NOT NULL,
    organization VARCHAR(128) NOT NULL,
    checksum VARCHAR(255) NOT NULL,
    uri VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT mh_archive_asset_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE TABLE mh_archive_episode
(
    id VARCHAR(128) NOT NULL,
    version BIGINT NOT NULL,
    organization VARCHAR(128) DEFAULT '' NOT NULL,
    deleted TINYINT DEFAULT 0 NOT NULL,
    access_control LONGTEXT,
    mediapackage_xml LONGTEXT,
    modification_date DATETIME,
    PRIMARY KEY (id, version, organization),
    CONSTRAINT mh_archive_episode_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE TABLE mh_archive_version_claim
(
    mediapackage VARCHAR(128) PRIMARY KEY NOT NULL,
    last_claimed BIGINT NOT NULL
);

CREATE UNIQUE INDEX UNQ_mh_archive_asset_0 ON mh_archive_asset (organization, mediapackage, mediapackageelement, version);
CREATE INDEX IX_mh_archive_asset_checksum ON mh_archive_asset (checksum);
CREATE INDEX IX_mh_archive_asset_mediapackage ON mh_archive_asset (mediapackage);
CREATE INDEX IX_mh_archive_asset_uri ON mh_archive_asset (uri);
CREATE INDEX FK_mh_archive_episode_organization ON mh_archive_episode (organization);
CREATE INDEX IX_mh_archive_episode_deleted ON mh_archive_episode (deleted);
CREATE INDEX IX_mh_archive_episode_mediapackage ON mh_archive_episode (id);
CREATE INDEX IX_mh_archive_episode_version ON mh_archive_episode (version);
CREATE INDEX IX_mh_archive_version_claim_last_claimed ON mh_archive_version_claim (last_claimed);
CREATE INDEX IX_mh_archive_version_claim_mediapackage ON mh_archive_version_claim (mediapackage);
