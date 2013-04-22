-- drop outdated constraints

ALTER TABLE job DROP FOREIGN KEY FK_job_root_id;
ALTER TABLE job DROP FOREIGN KEY FK_job_parent_id;
ALTER TABLE job DROP FOREIGN KEY FK_job_creator_service;
ALTER TABLE service_registration DROP FOREIGN KEY FK_service_registration_host_registration;

-- change engine to innodb

ALTER TABLE annotation ENGINE = InnoDB;
ALTER TABLE capture_agent_role ENGINE = InnoDB;
ALTER TABLE capture_agent_state ENGINE = InnoDB;
ALTER TABLE dictionary ENGINE = InnoDB;
ALTER TABLE host_registration ENGINE = InnoDB;
ALTER TABLE service_registration ENGINE = InnoDB;
ALTER TABLE job ENGINE = InnoDB;
ALTER TABLE job_arguments ENGINE = InnoDB;
ALTER TABLE job_context ENGINE = InnoDB;
ALTER TABLE matterhorn_user ENGINE = InnoDB;
ALTER TABLE matterhorn_role ENGINE = InnoDB;
ALTER TABLE scheduled_event ENGINE = InnoDB;
ALTER TABLE series ENGINE = InnoDB;
ALTER TABLE upload ENGINE = InnoDB;
ALTER TABLE user_action ENGINE = InnoDB;
ALTER TABLE oaipmh_harvesting ENGINE = InnoDB;

-- table naming convention has changed
alter table dictionary rename to mh_dictionary;
alter table host_registration rename to mh_host_registration;
alter table job  rename to mh_job;
alter table job_arguments rename to mh_job_argument;
alter table matterhorn_role rename to mh_matterhorn_role;
alter table scheduled_event rename to mh_scheduled_event;
alter table series  rename to mh_series;
alter table service_registration rename to mh_service_registration;
alter table matterhorn_user rename to mh_matterhorn_user;
alter table capture_agent_role rename to mh_capture_agent_role;
alter table capture_agent_state rename to mh_capture_agent_state;
alter table annotation rename to mh_annotation;
alter table job_context rename to mh_job_context;
alter table upload rename to mh_upload;
alter table user_action rename to mh_user_action;
alter table oaipmh_harvesting rename to mh_oaipmh_harvesting;


-- how to handle user & role? May be in & in 1.4 ddl but not in 1.3 ddl now mh_user and mh_role
CREATE TABLE IF NOT EXISTS mh_user (
  username VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  password TEXT(65535),
  PRIMARY KEY (username, organization),
  CONSTRAINT FK_mh_user_organization FOREIGN KEY (organization) REFERENCES mh_organization (id)
) ENGINE=InnoDB;

CREATE TABLE  IF NOT EXISTS mh_role (
  username VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  role TEXT(65535),
  CONSTRAINT FK_mh_role_username FOREIGN KEY (username, organization) REFERENCES mh_user (username, organization),
  CONSTRAINT FK_mh_role_organization FOREIGN KEY (organization) REFERENCES mh_organization (id)
) ENGINE=InnoDB;

CREATE INDEX IX_mh_role_pk ON mh_role (username, organization);

-- new tables for Episode Serivice
-- taken from the DDL script for 1.4
-- Organization Tables

CREATE TABLE mh_organization (
  id VARCHAR(128) NOT NULL,
  anonymous_role VARCHAR(255),
  name VARCHAR(255),
  admin_role VARCHAR(255),
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE mh_organization_node (
  organization VARCHAR(128) NOT NULL,
  port int(11),
  name VARCHAR(255),
  PRIMARY KEY (organization, port, name),
  CONSTRAINT FK_mh_organization_node_organization FOREIGN KEY (organization) REFERENCES mh_organization (id)
) ENGINE=InnoDB;

CREATE TABLE mh_episode_episode (
  id VARCHAR(128) NOT NULL,
  version BIGINT(20) NOT NULL,
  organization VARCHAR(128),
  deletion_date DATETIME,
  access_control TEXT(65535),
  mediapackage_xml TEXT(65535),
  modification_date DATETIME,
  PRIMARY KEY (id, version, organization),
  CONSTRAINT FK_mh_episode_episode_organization FOREIGN KEY (organization) REFERENCES mh_organization (id)
) ENGINE=InnoDB;

CREATE INDEX IX_mh_episode_episode_mediapackage ON mh_episode_episode (id);
CREATE INDEX IX_mh_episode_episode_version ON mh_episode_episode (version);

CREATE TABLE mh_episode_asset (
  id BIGINT(20) NOT NULL,
  mediapackageelement VARCHAR(128) NOT NULL,
  mediapackage VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  checksum VARCHAR(255) NOT NULL,
  uri VARCHAR(255) NOT NULL,
  version BIGINT(20) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_episode_asset_0 UNIQUE (organization, mediapackage, mediapackageelement, version),
  CONSTRAINT FK_mh_episode_asset_organization FOREIGN KEY (organization) REFERENCES mh_organization (id)
) ENGINE=InnoDB;

CREATE INDEX IX_mh_episode_asset_mediapackage ON mh_episode_asset (mediapackage);
CREATE INDEX IX_mh_episode_asset_checksum ON mh_episode_asset (checksum);
CREATE INDEX IX_mh_episode_asset_uri ON mh_episode_asset (uri);

CREATE TABLE mh_episode_version_claim (
 mediapackage VARCHAR(128) NOT NULL,
 last_claimed BIGINT(20) NOT NULL,
 PRIMARY KEY (mediapackage)
) ENGINE=InnoDB;

CREATE INDEX IX_mh_episode_version_claim_mediapackage ON mh_episode_version_claim (mediapackage);
CREATE INDEX IX_mh_episode_version_claim_last_claimed ON mh_episode_version_claim (last_claimed);


-- MH-8647
CREATE TABLE mh_search (
  id VARCHAR(128) NOT NULL,
  organization VARCHAR(128),
  deletion_date DATETIME,
  access_control TEXT(65535),
  mediapackage_xml TEXT(65535),
  modification_date DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT FK_mh_search_organization FOREIGN KEY (organization) REFERENCES mh_organization (id)
) ENGINE=InnoDB;



-- MH-8648 UCB Features
alter table mh_service_registration add column `ONLINE_FROM` DATETIME default NULL;
alter table mh_service_registration add column `SERVICE_STATE` VARCHAR(32) NOT NULL default 'NORMAL';
alter table mh_service_registration add column `STATE_CHANGED` DATETIME default NULL;
alter table mh_service_registration add column `WARNING_STATE_TRIGGER` BIGINT default 0;
alter table mh_service_registration add column `ERROR_STATE_TRIGGER` BIGINT default 0;


CREATE INDEX IX_mh_organization_node_pk ON mh_organization_node (organization);
CREATE INDEX IX_mh_organization_node_name ON mh_organization_node (name);
CREATE INDEX IX_mh_organization_node_port ON mh_organization_node (port);

CREATE TABLE mh_organization_property (
  organization VARCHAR(128) NOT NULL,
  name VARCHAR(255),
  value VARCHAR(255),
  PRIMARY KEY (organization, name),
  CONSTRAINT FK_mh_organization_property_organization FOREIGN KEY (organization) REFERENCES mh_organization (id)
) ENGINE=InnoDB;

CREATE INDEX IX_mh_organization_property_pk ON mh_organization_property (organization);



-- job-service table
-- note missing from default ddl script
CREATE TABLE mh_job_mh_service_registration (
  Job_id bigint(20) NOT NULL,
  servicesRegistration_id bigint(20) NOT NULL,
  PRIMARY KEY (`Job_id`,`servicesRegistration_id`),
  KEY `mhjobmhservice_registrationservicesRegistration_id` (`servicesRegistration_id`),
  CONSTRAINT `FK_mh_job_mh_service_registration_Job_id` FOREIGN KEY (`Job_id`) REFERENCES `mh_job` (`id`),
  CONSTRAINT `mhjobmhservice_registrationservicesRegistration_id` FOREIGN KEY (`servicesRegistration_id`) REFERENCES `mh_service_registration` (`id`)
) ENGINE=InnoDB;

-- MH-8854 user_action text fields

alter table mh_user_action change column user_ip user_ip varchar(255);
alter table mh_user_action change column mediapackage_id mediapackage_id varchar(255);
alter table mh_user_action change column session_id session_id varchar(255);
alter table mh_user_action change column user_id user_id varchar(255);
alter table mh_user_action change column type type varchar(255);

create index user_action_session_type_i on mh_user_action(session_id, type);


-- rename fields

ALTER TABLE mh_annotation CHANGE mediapackage_id mediapackage VARCHAR(128);
ALTER TABLE mh_annotation CHANGE session_id session VARCHAR(128);
ALTER TABLE mh_annotation CHANGE user_id user VARCHAR(255);
ALTER TABLE mh_annotation CHANGE annotation_type type VARCHAR(128);
ALTER TABLE mh_annotation CHANGE annotation_value value TEXT(65535);
-- ALTER TABLE mh_annotation CHANGE private_annotation private TINYINT(1) DEFAULT 0;
ALTER TABLE mh_job CHANGE parent_id parent BIGINT;
ALTER TABLE mh_job CHANGE root_id root BIGINT;
ALTER TABLE mh_job CHANGE is_dispatchable dispatchable TINYINT(1);
ALTER TABLE mh_job CHANGE organization organization VARCHAR(128);
ALTER TABLE mh_job CHANGE operation operation VARCHAR(128);
ALTER TABLE mh_job_argument CHANGE list_index argument_index INTEGER;
ALTER TABLE mh_job_context CHANGE root_job id BIGINT;
ALTER TABLE mh_job_context CHANGE key_entry name VARCHAR(255);
ALTER TABLE mh_scheduled_event CHANGE event_id id BIGINT;
ALTER TABLE mh_series CHANGE series_id id VARCHAR(128) NOT NULL;
ALTER TABLE mh_series CHANGE organization_id organization VARCHAR(128) NOT NULL;
ALTER TABLE mh_user_action CHANGE mediapackage_id mediapackage VARCHAR(128);
ALTER TABLE mh_user_action CHANGE session_id session VARCHAR(128);
ALTER TABLE mh_user_action CHANGE user_id user VARCHAR(255);
ALTER TABLE mh_user_action CHANGE is_playing playing TINYINT(1) DEFAULT 0;
ALTER TABLE mh_user_action CHANGE type type VARCHAR(128);

-- add fields

ALTER TABLE mh_annotation ADD COLUMN private TINYINT(1) DEFAULT 1;
ALTER TABLE mh_host_registration ADD COLUMN active TINYINT(1) DEFAULT 1;
ALTER TABLE mh_service_registration ADD COLUMN active TINYINT(1) DEFAULT 1;
ALTER TABLE mh_service_registration ADD COLUMN service_state VARCHAR(32) NOT NULL default 'NORMAL';
ALTER TABLE mh_service_registration ADD COLUMN state_changed DATETIME;
ALTER TABLE mh_service_registration ADD COLUMN warning_state_trigger BIGINT;
ALTER TABLE mh_service_registration ADD COLUMN error_state_trigger BIGINT;
ALTER TABLE mh_service_registration ADD COLUMN online_from DATETIME;


-- add primary keys

ALTER TABLE mh_capture_agent_role ADD PRIMARY KEY (id, organization, role);

-- add constraints

ALTER TABLE mh_capture_agent_role ADD CONSTRAINT FK_mh_capture_agent_role_organization FOREIGN KEY (organization) REFERENCES mh_organization (id);
ALTER TABLE mh_capture_agent_state ADD CONSTRAINT FK_mh_capture_agent_state_organization FOREIGN KEY (organization) REFERENCES mh_organization (id);
ALTER TABLE mh_host_registration ADD CONSTRAINT UNQ_mh_host_registration_0 UNIQUE (host);
ALTER TABLE mh_service_registration ADD CONSTRAINT FK_service_registration_host_registration FOREIGN KEY (host_registration) REFERENCES mh_host_registration (id);
ALTER TABLE mh_job ADD CONSTRAINT FK_mh_job_creator_service FOREIGN KEY (creator_service) REFERENCES mh_service_registration (id);
ALTER TABLE mh_job ADD CONSTRAINT FK_mh_job_processor_service FOREIGN KEY (processor_service) REFERENCES mh_service_registration (id);
ALTER TABLE mh_job ADD CONSTRAINT FK_mh_job_parent FOREIGN KEY (parent) REFERENCES mh_job (id);
ALTER TABLE mh_job ADD CONSTRAINT FK_mh_job_root FOREIGN KEY (root) REFERENCES mh_job (id);
ALTER TABLE mh_job ADD CONSTRAINT FK_mh_job_organization FOREIGN KEY (organization) REFERENCES mh_organization (id);
ALTER TABLE mh_job_argument ADD CONSTRAINT FK_mh_job_argument_id FOREIGN KEY (id) REFERENCES mh_job (id);
ALTER TABLE mh_job_context ADD CONSTRAINT FK_mh_job_context_id FOREIGN KEY (id) REFERENCES mh_job (id);
ALTER TABLE mh_user ADD CONSTRAINT FK_mh_user_organization FOREIGN KEY (organization) REFERENCES mh_organization (id);
ALTER TABLE mh_role ADD CONSTRAINT FK_mh_role_username FOREIGN KEY (username, organization) REFERENCES mh_user (username, organization);
ALTER TABLE mh_role ADD CONSTRAINT FK_mh_role_organization FOREIGN KEY (organization) REFERENCES mh_organization (id);
ALTER TABLE mh_series ADD CONSTRAINT FK_mh_series_organization FOREIGN KEY (organization) REFERENCES mh_organization (id);

-- add indices

CREATE INDEX IX_mh_annotation_created ON mh_annotation (created);
CREATE INDEX IX_mh_annotation_inpoint ON mh_annotation (inpoint);
CREATE INDEX IX_mh_annotation_outpoint ON mh_annotation (outpoint);
CREATE INDEX IX_mh_annotation_mediapackage ON mh_annotation (mediapackage);
CREATE INDEX IX_mh_annotation_private ON mh_annotation (private);
CREATE INDEX IX_mh_annotation_user ON mh_annotation (user);
CREATE INDEX IX_mh_annotation_session ON mh_annotation (session);
CREATE INDEX IX_mh_annotation_type ON mh_annotation (type);
CREATE INDEX IX_mh_capture_agent_role ON mh_capture_agent_role (id, organization);
CREATE INDEX IX_mh_dictionary_weight ON mh_dictionary (weight);
CREATE INDEX IX_mh_host_registration_online ON mh_host_registration (online);
CREATE INDEX IX_mh_host_registration_active ON mh_host_registration (active);
CREATE INDEX IX_mh_service_registration_service_type ON mh_service_registration (service_type);
CREATE INDEX IX_mh_service_registration_service_state ON mh_service_registration (service_state);
CREATE INDEX IX_mh_service_registration_active ON mh_service_registration (active);
CREATE INDEX IX_mh_service_registration_host_registration ON mh_service_registration (host_registration);
CREATE INDEX IX_mh_job_parent ON mh_job (parent);
CREATE INDEX IX_mh_job_root ON mh_job (root);
CREATE INDEX IX_mh_job_creator_service ON mh_job (creator_service);
CREATE INDEX IX_mh_job_processor_service ON mh_job (processor_service);
CREATE INDEX IX_mh_job_status ON mh_job (status);
CREATE INDEX IX_mh_job_date_created ON mh_job (date_created);
CREATE INDEX IX_mh_job_date_completed ON mh_job (date_completed);
CREATE INDEX IX_mh_job_dispatchable ON mh_job (dispatchable);
CREATE INDEX IX_mh_job_operation ON mh_job (operation);
CREATE INDEX IX_mh_job_argument_id ON mh_job_argument (id);
CREATE INDEX IX_mh_job_context_id ON mh_job_context (id);
CREATE INDEX IX_mh_role_pk ON mh_role (username, organization);
CREATE INDEX IX_mh_user_action_created ON mh_user_action (created);
CREATE INDEX IX_mh_user_action_inpoint ON mh_user_action (inpoint);
CREATE INDEX IX_mh_user_action_outpoint ON mh_user_action (outpoint);
CREATE INDEX IX_mh_user_action_mediapackage ON mh_user_action (mediapackage);
CREATE INDEX IX_mh_user_action_user ON mh_user_action (user);
CREATE INDEX IX_mh_user_action_session ON mh_user_action (session);
CREATE INDEX IX_mh_user_action_type ON mh_user_action (type);



