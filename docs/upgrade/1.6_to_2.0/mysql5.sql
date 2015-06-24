use matterhorn;

CREATE TABLE mh_user_session (
  session_id VARCHAR(50) NOT NULL,
  user_ip VARCHAR(255),
  user_agent VARCHAR(255),
  user_id VARCHAR(255),
  PRIMARY KEY (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

#Copy over the relevant session data
INSERT INTO mh_user_session (session_id, user_ip, user_id) SELECT session, user_id, user_ip FROM mh_user_action GROUP BY session;

ALTER TABLE mh_user_action CHANGE session session_id VARCHAR(50);

DROP INDEX IX_mh_user_action_user_id ON mh_user_action;
DROP INDEX IX_mh_user_action_session_id ON mh_user_action;

ALTER TABLE mh_user_action DROP COLUMN user_id;
ALTER TABLE mh_user_action DROP COLUMN user_ip;

ALTER TABLE mh_user_action ADD CONSTRAINT FK_mh_user_action_session_id FOREIGN KEY (session_id) REFERENCES mh_user_session (session_id);

CREATE INDEX IX_mh_user_session_user_id ON mh_user_session (user_id);

ALTER TABLE mh_organization_property MODIFY value TEXT(65535);

-- Alter Existing Tables
-- Adding default values for the columns that will be overwritten at startup
ALTER TABLE mh_host_registration ADD COLUMN address VARCHAR(39) DEFAULT '127.0.0.1' NOT NULL;
ALTER TABLE mh_host_registration ADD COLUMN memory bigint DEFAULT 0 NOT NULL;
ALTER TABLE mh_host_registration ADD COLUMN cores integer DEFAULT 0 NOT NULL;

ALTER TABLE mh_scheduled_event
ADD COLUMN `mediapackage_id` VARCHAR(128) AFTER `id`,
ADD COLUMN `access_control` TEXT(65535) AFTER `dublin_core`,
ADD COLUMN `opt_out` TINYINT(1) NOT NULL DEFAULT '0' AFTER `access_control`,
ADD COLUMN `blacklisted` TINYINT(1) NOT NULL DEFAULT '0' AFTER `opt_out`,
ADD COLUMN `review_status` VARCHAR(255) DEFAULT NULL AFTER `blacklisted`,
ADD COLUMN `review_date` DATETIME DEFAULT NULL AFTER `review_status`;

ALTER TABLE mh_series
ADD COLUMN `opt_out` tinyint(1) NOT NULL DEFAULT '0' AFTER `dublin_core`;

ALTER TABLE mh_user
ADD COLUMN name varchar(256) DEFAULT NULL AFTER `password`,
ADD COLUMN email varchar(256) DEFAULT NULL AFTER `name`;

-- Create New Tables

CREATE TABLE mh_series_property (
  `organization` VARCHAR(128) NOT NULL,
  `series` VARCHAR(128) NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `value` TEXT(65535),
  PRIMARY KEY (`organization`, `series`, `name`),
  CONSTRAINT FK_mh_series_property_series FOREIGN KEY (series) REFERENCES mh_series (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

CREATE INDEX IX_mh_series_property_pk ON mh_series_property (series);

CREATE TABLE mh_user_settings (
  id bigint(20) NOT NULL,
  setting_key VARCHAR(255) NOT NULL,
  setting_value text NOT NULL,
  username varchar(128) DEFAULT NULL,
  organization varchar(128) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FK_mh_user_setting_username FOREIGN KEY (username) REFERENCES mh_user (username),
  CONSTRAINT FK_mh_user_setting_org FOREIGN KEY (organization) REFERENCES mh_user (organization)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

CREATE TABLE mh_email_configuration (
  id BIGINT(20) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  port INT(5) DEFAULT NULL,
  transport VARCHAR(255) DEFAULT NULL,
  username VARCHAR(255) DEFAULT NULL,
  server VARCHAR(255) NOT NULL,
  ssl_enabled TINYINT(1) NOT NULL DEFAULT '0',
  password VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_email_configuration_0 UNIQUE (organization),
  CONSTRAINT FK_mh_email_configuration_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

CREATE INDEX IX_mh_email_configuration_organization ON mh_email_configuration (organization);

CREATE TABLE mh_comment (
  id BIGINT(20) NOT NULL,
  creation_date DATETIME NOT NULL,
  author VARCHAR(255) NOT NULL,
  text VARCHAR(255) NOT NULL,
  reason VARCHAR(255) DEFAULT NULL,
  modification_date DATETIME NOT NULL,
  resolved_status TINYINT(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

CREATE INDEX IX_mh_comment_author ON mh_comment (author);
CREATE INDEX IX_mh_comment_resolved_status ON mh_comment (resolved_status);

CREATE TABLE mh_comment_reply (
  id BIGINT(20) NOT NULL,
  creation_date DATETIME NOT NULL,
  author VARCHAR(255) NOT NULL,
  text VARCHAR(255) NOT NULL,
  modification_date DATETIME NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

CREATE INDEX IX_mh_comment_reply_author ON mh_comment_reply (author);

CREATE TABLE mh_comment_mh_comment_reply (
  Comment_id BIGINT(20) NOT NULL,
  replies_id BIGINT(20) NOT NULL,
  PRIMARY KEY (Comment_id,replies_id),
  KEY FK_mh_comment_mh_comment_reply_replies_id (replies_id),
  CONSTRAINT FK_mh_comment_mh_comment_reply_Comment_id FOREIGN KEY (Comment_id) REFERENCES mh_comment (id),
  CONSTRAINT FK_mh_comment_mh_comment_reply_replies_id FOREIGN KEY (replies_id) REFERENCES mh_comment_reply (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

CREATE TABLE mh_message_signature (
  id BIGINT(20) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  name VARCHAR(255) NOT NULL,
  creation_date DATETIME NOT NULL,
  sender VARCHAR(255) NOT NULL,
  sender_name VARCHAR(255) NOT NULL,
  reply_to VARCHAR(255) DEFAULT NULL,
  reply_to_name VARCHAR(255) DEFAULT NULL,
  signature VARCHAR(255) NOT NULL,
  creator_username VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_message_signature_0 UNIQUE (organization, name),
  CONSTRAINT FK_mh_message_signature_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

CREATE INDEX IX_mh_message_signature_organization ON mh_message_signature (organization);
CREATE INDEX IX_mh_message_signature_name ON mh_message_signature (name);

CREATE TABLE mh_message_signature_mh_comment (
  MessageSignature_id BIGINT(20) NOT NULL,
  comments_id BIGINT(20) NOT NULL,
  PRIMARY KEY (MessageSignature_id, comments_id),
  KEY mh_message_signature_mh_comment_comments_id (comments_id),
  CONSTRAINT mh_message_signature_mh_comment_comments_id FOREIGN KEY (comments_id) REFERENCES mh_comment (id) ON DELETE CASCADE,
  CONSTRAINT mhmessagesignaturemhcommentMessageSignature_id FOREIGN KEY (MessageSignature_id) REFERENCES mh_message_signature (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

CREATE TABLE mh_message_template (
  id BIGINT(20) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  body TEXT(65535) NOT NULL,
  creation_date DATETIME NOT NULL,
  subject VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  TYPE VARCHAR(255) DEFAULT NULL,
  creator_username VARCHAR(255) NOT NULL,
  hidden TINYINT(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_message_template_0 UNIQUE (organization, name),
  CONSTRAINT FK_mh_message_template_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

CREATE INDEX IX_mh_message_template_organization ON mh_message_template (organization);
CREATE INDEX IX_mh_message_template_name ON mh_message_template (name);

CREATE TABLE mh_message_template_mh_comment (
  MessageTemplate_id BIGINT(20) NOT NULL,
  comments_id BIGINT(20) NOT NULL,
  PRIMARY KEY (MessageTemplate_id, comments_id),
  KEY FK_mh_message_template_mh_comment_comments_id (comments_id),
  CONSTRAINT mhmessagetemplatemh_commentMessageTemplate_id FOREIGN KEY (MessageTemplate_id) REFERENCES mh_message_template (id) ON DELETE CASCADE,
  CONSTRAINT mh_message_template_mh_comment_comments_id FOREIGN KEY (comments_id) REFERENCES mh_comment (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

--
-- Archive
--

-- rename episode table names to archive

RENAME TABLE mh_episode_asset TO mh_archive_asset;
RENAME TABLE mh_episode_episode TO mh_archive_episode;
RENAME TABLE mh_episode_version_claim TO mh_archive_version_claim;

-- rename foreign key and constraints
ALTER TABLE mh_archive_asset DROP FOREIGN KEY FK_mh_episode_asset_organization;
ALTER TABLE mh_archive_asset DROP INDEX UNQ_mh_episode_asset_0;
ALTER TABLE mh_archive_asset ADD CONSTRAINT UNQ_mh_archive_asset_0 UNIQUE (organization, mediapackage, mediapackageelement, version);
ALTER TABLE mh_archive_asset ADD CONSTRAINT FK_mh_archive_asset_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE;

ALTER TABLE mh_archive_episode DROP FOREIGN KEY FK_mh_episode_episode_organization;
ALTER TABLE mh_archive_episode ADD CONSTRAINT FK_mh_archive_episode_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE;

-- rename indizies
ALTER TABLE mh_archive_asset DROP INDEX IX_mh_episode_asset_mediapackage;
ALTER TABLE mh_archive_asset ADD INDEX IX_mh_archive_asset_mediapackage (mediapackage);
ALTER TABLE mh_archive_asset DROP INDEX IX_mh_episode_asset_checksum;
ALTER TABLE mh_archive_asset ADD INDEX IX_mh_archive_asset_checksum (checksum);
ALTER TABLE mh_archive_asset DROP INDEX IX_mh_episode_asset_uri;
ALTER TABLE mh_archive_asset ADD INDEX IX_mh_archive_asset_uri (uri);

ALTER TABLE mh_archive_episode DROP INDEX IX_mh_episode_episode_mediapackage;
ALTER TABLE mh_archive_episode ADD INDEX IX_mh_archive_episode_mediapackage (id);
ALTER TABLE mh_archive_episode DROP INDEX IX_mh_episode_episode_version;
ALTER TABLE mh_archive_episode ADD INDEX IX_mh_archive_episode_version (version);

ALTER TABLE mh_archive_version_claim DROP INDEX IX_mh_episode_version_claim_mediapackage;
ALTER TABLE mh_archive_version_claim ADD INDEX IX_mh_archive_version_claim_mediapackage (mediapackage);
ALTER TABLE mh_archive_version_claim DROP INDEX IX_mh_episode_version_claim_last_claimed;
ALTER TABLE mh_archive_version_claim ADD INDEX IX_mh_archive_version_claim_last_claimed (last_claimed);

-- remove deletion_date column and add deleted column with values regarding to the deletion_date

ALTER TABLE mh_archive_episode ADD COLUMN deleted TINYINT(1) DEFAULT 0 NOT NULL;
UPDATE mh_archive_episode SET deleted=TRUE WHERE deletion_date IS NOT NULL;
ALTER TABLE mh_archive_episode DROP COLUMN deletion_date;

-- create new deleted index

CREATE INDEX IX_mh_archive_episode_deleted ON mh_archive_episode (deleted);

--
-- Admin UI next generation
--
CREATE TABLE mh_event_mh_comment (
  id BIGINT(20) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  event VARCHAR(128) NOT NULL,
  comment BIGINT(20) NOT NULL,
  PRIMARY KEY (id),
  KEY FK_mh_event_mh_comment_comment (comment),
  CONSTRAINT FK_mh_event_mh_comment_comment FOREIGN KEY (comment) REFERENCES mh_comment (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

CREATE TABLE mh_series_elements (
    series VARCHAR(128) NOT NULL,
    organization VARCHAR(128) NOT NULL,
    type VARCHAR(128) NOT NULL,
    data BLOB,
    PRIMARY KEY (series, organization, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 

CREATE TABLE mh_themes (
    id BIGINT(20) NOT NULL,
    organization VARCHAR(128) NOT NULL,
    creation_date DATETIME NOT NULL,
    username VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL,
    isDefault tinyint(1) NOT NULL DEFAULT '0',
    description VARCHAR(255),
    bumper_active tinyint(1) NOT NULL DEFAULT '0',
    bumper_file VARCHAR(128),
    license_slide_active tinyint(1) NOT NULL DEFAULT '0',
    license_slide_background VARCHAR(128),
    license_slide_description VARCHAR(255),
    title_slide_active tinyint(1) NOT NULL DEFAULT '0',
    title_slide_background VARCHAR(128),
    title_slide_metadata VARCHAR(255),
    trailer_active tinyint(1) NOT NULL DEFAULT '0',
    trailer_file VARCHAR(128),
    watermark_active tinyint(1) NOT NULL DEFAULT '0',
    watermark_position VARCHAR(255),
    watermark_file VARCHAR(128),
    PRIMARY KEY (id),
    CONSTRAINT FK_mh_themes_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
