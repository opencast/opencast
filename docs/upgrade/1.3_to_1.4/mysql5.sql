/*
 * MySQL 5 upgrade script for Matterhorn 1.3 to trunk
 */

/**
 * MH-8648 UCB Features
 */
alter table `SERVICE_REGISTRATION` add column `ONLINE_FROM` DATETIME default NULL;
alter table `SERVICE_REGISTRATION` add column `SERVICE_STATE` VARCHAR(32) NOT NULL default 'NORMAL';
alter table `SERVICE_REGISTRATION` add column `STATE_CHANGED` DATETIME default NULL;
alter table `SERVICE_REGISTRATION` add column `WARNING_STATE_TRIGGER` BIGINT default 0;
alter table `SERVICE_REGISTRATION` add column `ERROR_STATE_TRIGGER` BIGINT default 0;