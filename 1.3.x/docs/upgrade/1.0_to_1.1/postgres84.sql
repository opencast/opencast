/*
 * PostgreSQL 8.4 upgrade script for Matterhorn 1.0 to 1.1
 */

/**
 * MH-5445 (overview of series and services)
 */
alter table service_registration add column online boolean NOT NULL default 'f';
alter table service_registration add column path varying(255) NOT NULL;
alter table job rename column type to job_type;
alter table job add column runtime bigint;
alter table job add column queuetime bigint;
alter table job add FOREIGN KEY (job_type, host) REFERENCES service_registration(job_type, host);

/**
 * MH-5364 (DB schema fixes for the feedback service)
 */
alter table annotation add column user_id varchar(255) default NULL;
alter table annotation alter column  media_package_id type varchar(36);
create index media_package_idx on annotation (media_package_id);
