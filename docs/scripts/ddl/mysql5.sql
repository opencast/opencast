create schema matterhorn DEFAULT CHARACTER SET = 'utf8';
use matterhorn;

CREATE TABLE SEQUENCE (
  SEQ_NAME VARCHAR(50) NOT NULL,
  SEQ_COUNT DECIMAL(38),
  PRIMARY KEY (SEQ_NAME)
) ENGINE=InnoDB;

INSERT INTO SEQUENCE(SEQ_NAME, SEQ_COUNT) values ('SEQ_GEN', 0);

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
  CONSTRAINT FK_mh_organization_node_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX IX_mh_organization_node_pk ON mh_organization_node (organization);
CREATE INDEX IX_mh_organization_node_name ON mh_organization_node (name);
CREATE INDEX IX_mh_organization_node_port ON mh_organization_node (port);

CREATE TABLE mh_organization_property (
  organization VARCHAR(128) NOT NULL,
  name VARCHAR(255),
  value VARCHAR(255),
  PRIMARY KEY (organization, name),
  CONSTRAINT FK_mh_organization_property_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX IX_mh_organization_property_pk ON mh_organization_property (organization);

CREATE TABLE mh_annotation (
  id BIGINT NOT NULL,
  inpoint INTEGER,
  outpoint INTEGER,
  mediapackage VARCHAR(128),
  session VARCHAR(128),
  created DATETIME,
  user VARCHAR(255),
  length INTEGER,
  type VARCHAR(128),
  value TEXT(65535),
  private TINYINT(1) DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX IX_mh_annotation_created ON mh_annotation (created);
CREATE INDEX IX_mh_annotation_inpoint ON mh_annotation (inpoint);
CREATE INDEX IX_mh_annotation_outpoint ON mh_annotation (outpoint);
CREATE INDEX IX_mh_annotation_mediapackage ON mh_annotation (mediapackage);
CREATE INDEX IX_mh_annotation_private ON mh_annotation (private);
CREATE INDEX IX_mh_annotation_user ON mh_annotation (user);
CREATE INDEX IX_mh_annotation_session ON mh_annotation (session);
CREATE INDEX IX_mh_annotation_type ON mh_annotation (type);

CREATE TABLE mh_capture_agent_role (
  id VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  role VARCHAR(255),
  PRIMARY KEY (id, organization, role),
  CONSTRAINT FK_mh_capture_agent_role_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX IX_mh_capture_agent_role_pk ON mh_capture_agent_role (id, organization);

CREATE TABLE mh_capture_agent_state (
  id VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  configuration TEXT(65535),
  state TEXT(65535) NOT NULL,
  last_heard_from BIGINT NOT NULL,
  url TEXT(65535),
  PRIMARY KEY (id, organization),
  CONSTRAINT FK_mh_capture_agent_state_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE mh_dictionary (
  text VARCHAR(255) NOT NULL,
  language VARCHAR(5) NOT NULL,
  weight DOUBLE,
  count BIGINT,
  stop_word TINYINT(1) DEFAULT 0,
  PRIMARY KEY (text, language)
) ENGINE=InnoDB;

CREATE INDEX IX_mh_dictionary_weight ON mh_dictionary (weight);

CREATE TABLE mh_host_registration (
  id BIGINT NOT NULL,
  host VARCHAR(255) NOT NULL,
  maintenance TINYINT(1) DEFAULT 0 NOT NULL,
  online TINYINT(1) DEFAULT 1 NOT NULL,
  active TINYINT(1) DEFAULT 1 NOT NULL,
  max_jobs INTEGER NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_host_registration_0 UNIQUE (host)
) ENGINE=InnoDB;

CREATE INDEX IX_mh_host_registration_online ON mh_host_registration (online);
CREATE INDEX IX_mh_host_registration_active ON mh_host_registration (active);

CREATE TABLE mh_service_registration (
  id BIGINT NOT NULL,
  path VARCHAR(255) NOT NULL,
  job_producer TINYINT(1) DEFAULT 0 NOT NULL,
  service_type VARCHAR(255) NOT NULL,
  online TINYINT(1) DEFAULT 1 NOT NULL,
  active TINYINT(1) DEFAULT 1 NOT NULL,
  online_from DATETIME,
  service_state int NOT NULL,
  state_changed DATETIME,
  warning_state_trigger BIGINT,
  error_state_trigger BIGINT,
  host_registration BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_service_registration_0 UNIQUE (host_registration, service_type),
  CONSTRAINT FK_service_registration_host_registration FOREIGN KEY (host_registration) REFERENCES mh_host_registration (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX IX_mh_service_registration_service_type ON mh_service_registration (service_type);
CREATE INDEX IX_mh_service_registration_service_state ON mh_service_registration (service_state);
CREATE INDEX IX_mh_service_registration_active ON mh_service_registration (active);
CREATE INDEX IX_mh_service_registration_host_registration ON mh_service_registration (host_registration);

CREATE TABLE mh_job (
  id BIGINT NOT NULL,
  status INTEGER,
  payload TEXT(65535),
  date_started DATETIME,
  run_time BIGINT,
  creator TEXT(65535) NOT NULL,
  instance_version BIGINT,
  date_completed DATETIME,
  operation VARCHAR(128),
  dispatchable TINYINT(1) DEFAULT 1,
  organization VARCHAR(128) NOT NULL,
  date_created DATETIME,
  queue_time BIGINT,
  creator_service BIGINT,
  processor_service BIGINT,
  parent BIGINT,
  root BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT FK_mh_job_creator_service FOREIGN KEY (creator_service) REFERENCES mh_service_registration (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_job_processor_service FOREIGN KEY (processor_service) REFERENCES mh_service_registration (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_job_parent FOREIGN KEY (parent) REFERENCES mh_job (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_job_root FOREIGN KEY (root) REFERENCES mh_job (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_job_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX IX_mh_job_parent ON mh_job (parent);
CREATE INDEX IX_mh_job_root ON mh_job (root);
CREATE INDEX IX_mh_job_creator_service ON mh_job (creator_service);
CREATE INDEX IX_mh_job_processor_service ON mh_job (processor_service);
CREATE INDEX IX_mh_job_status ON mh_job (status);
CREATE INDEX IX_mh_job_date_created ON mh_job (date_created);
CREATE INDEX IX_mh_job_date_completed ON mh_job (date_completed);
CREATE INDEX IX_mh_job_dispatchable ON mh_job (dispatchable);
CREATE INDEX IX_mh_job_operation ON mh_job (operation);

CREATE TABLE mh_job_argument (
  id BIGINT NOT NULL,
  argument TEXT(2147483647),
  argument_index INTEGER,
  CONSTRAINT UNQ_job_argument_0 UNIQUE (id, argument_index),
  CONSTRAINT FK_job_argument_id FOREIGN KEY (id) REFERENCES mh_job (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX IX_mh_job_argument_id ON mh_job_argument (id);

CREATE TABLE mh_job_context (
  id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  value TEXT(65535),
  CONSTRAINT UNQ_job_context_0 UNIQUE (id, name),
  CONSTRAINT FK_job_context_id FOREIGN KEY (id) REFERENCES mh_job (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX IX_mh_job_context_id ON mh_job_context (id);

CREATE TABLE mh_user (
  username VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  password TEXT(65535),
  PRIMARY KEY (username, organization),
  CONSTRAINT FK_mh_user_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE mh_role (
  username VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  role TEXT(65535),
  CONSTRAINT FK_mh_role_username FOREIGN KEY (username, organization) REFERENCES mh_user (username, organization) ON DELETE CASCADE,
  CONSTRAINT FK_mh_role_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX IX_mh_role_pk ON mh_role (username, organization);

CREATE TABLE mh_scheduled_event (
  id BIGINT NOT NULL,
  capture_agent_metadata TEXT(65535),
  dublin_core TEXT(65535),
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE mh_search (
  id VARCHAR(128) NOT NULL,
  organization VARCHAR(128),
  deletion_date DATETIME,
  access_control TEXT(65535),
  mediapackage_xml TEXT(65535),
  modification_date DATETIME,
  PRIMARY KEY (id),
  CONSTRAINT FK_mh_search_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX IX_mh_search_organization ON mh_search (organization);

CREATE TABLE mh_series (
  id VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  access_control TEXT(65535),
  dublin_core TEXT(65535),
  PRIMARY KEY (id, organization),
  CONSTRAINT FK_mh_series_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE mh_upload (
  id VARCHAR(255) NOT NULL,
  total BIGINT NOT NULL,
  received BIGINT NOT NULL,
  filename TEXT(65535) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE mh_user_action (
  id BIGINT NOT NULL,
  user_ip VARCHAR(255),
  inpoint INTEGER,
  outpoint INTEGER,
  mediapackage VARCHAR(128),
  session VARCHAR(128),
  created DATETIME,
  user VARCHAR(255),
  length INTEGER,
  type VARCHAR(128),
  playing TINYINT(1) DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX IX_mh_user_action_created ON mh_user_action (created);
CREATE INDEX IX_mh_user_action_inpoint ON mh_user_action (inpoint);
CREATE INDEX IX_mh_user_action_outpoint ON mh_user_action (outpoint);
CREATE INDEX IX_mh_user_action_mediapackage_id ON mh_user_action (mediapackage);
CREATE INDEX IX_mh_user_action_user_id ON mh_user_action (user);
CREATE INDEX IX_mh_user_action_session_id ON mh_user_action (session);
CREATE INDEX IX_mh_user_action_type ON mh_user_action (type);

CREATE TABLE mh_oaipmh_harvesting (
  url VARCHAR(255) NOT NULL,
  last_harvested datetime,
  PRIMARY KEY (url)
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
  CONSTRAINT FK_mh_episode_episode_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
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
  CONSTRAINT FK_mh_episode_asset_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
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
