/*
 * MySQL 5 upgrade script for Matterhorn 1.0 to 1.1
 */

/**
 * MH-5445 (overview of series and services)
 */
alter table `SERVICE_REGISTRATION` add column `ONLINE` tinyint NOT NULL default '0';
alter table `SERVICE_REGISTRATION` add column `PATH` varchar(255) NOT NULL;
alter table `JOB` change `TYPE` `JOB_TYPE` varchar(255) collate utf8_unicode_ci default NULL;
alter table `JOB` add column `RUNTIME` bigint(20) default NULL;
alter table `JOB` add column `QUEUETIME` bigint(20) default NULL;
create index JOB_TYPE_HOST on `JOB` (`HOST`, `JOB_TYPE`);
alter table `JOB` add FOREIGN KEY (`HOST`, `JOB_TYPE`) REFERENCES `SERVICE_REGISTRATION` (`HOST`, `JOB_TYPE`);


/**
 * MH-5364 (DB schema fixes for the feedback service)
 */
alter table `ANNOTATION` add column `USER_ID` varchar(255) default NULL;
alter table `ANNOTATION` modify `MEDIA_PACKAGE_ID` VARCHAR(36);
create index MEDIA_PACKAGE_IDX on `ANNOTATION` (`MEDIA_PACKAGE_ID`);