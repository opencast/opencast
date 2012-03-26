/*
 * MySQL 5 upgrade script for Matterhorn 1.3 to 1.4
 */

/**
 * MH-8648 UCB Features
 */
alter table `SERVICE_REGISTRATION` add column `ONLINE_FROM` DATETIME default NULL;
alter table `SERVICE_REGISTRATION` add column `SERVICE_STATE` VARCHAR(32) NOT NULL default 'NORMAL';
alter table `SERVICE_REGISTRATION` add column `STATE_CHANGED` DATETIME default NULL;
alter table `SERVICE_REGISTRATION` add column `WARNING_STATE_TRIGGER` BIGINT default 0;
alter table `SERVICE_REGISTRATION` add column `ERROR_STATE_TRIGGER` BIGINT default 0;

/**
 * MH-8647
 */
CREATE TABLE search (
  mediapackage_id VARCHAR(128) NOT NULL,
  organization_id VARCHAR(128) DEFAULT NULL,
  deletion_date DATETIME DEFAULT NULL,
  access_control TEXT(65535),
  mediapackage TEXT(65535),
  modification_date DATETIME DEFAULT NULL,
  PRIMARY KEY (mediapackage_id)
);