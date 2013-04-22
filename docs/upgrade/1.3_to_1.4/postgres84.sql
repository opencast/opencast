
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

-- new tables for Episode Serivice
-- taken from the DDL script for 1.4
-- Organization Tables

CREATE TABLE "mh_organization" (
  "id" character varying(128) NOT NULL,
  "anonymous_role" character varying(255),
  "name" character varying(255),
  "admin_role" character varying(255),
  PRIMARY KEY ("id")
);

CREATE TABLE "mh_organization_node" (
  "organization" character varying(128) NOT NULL,
  "port" integer,
  "name" character varying(255),
  PRIMARY KEY ("organization", "port", "name"),
  CONSTRAINT "FK_mh_organization_node_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE INDEX "IX_mh_organization_node_pk" ON "mh_organization_node" ("organization");
CREATE INDEX "IX_mh_organization_node_name" ON "mh_organization_node" ("name");
CREATE INDEX "IX_mh_organization_node_port" ON "mh_organization_node" ("port");


CREATE TABLE "mh_episode_episode" (
  "id" character varying(128) NOT NULL,
  "version" bigint NOT NULL,
  "organization" character varying(128),
  "deletion_date" timestamp,
  "access_control" text,
  "mediapackage_xml" text,
  "modification_date" timestamp,
  PRIMARY KEY ("id", "version", "organization"),
  CONSTRAINT "FK_mh_episode_episode_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE INDEX "IX_mh_episode_episode_mediapackage" ON "mh_episode_episode" ("id");
CREATE INDEX "IX_mh_episode_episode_version" ON "mh_episode_episode" ("version");

CREATE TABLE "mh_episode_asset" (
  "id" bigint NOT NULL,
  "mediapackageelement" character varying(128) NOT NULL,
  "mediapackage" character varying(128) NOT NULL,
  "organization" character varying(128) NOT NULL,
  "checksum" character varying(255) NOT NULL,
  "uri" character varying(255) NOT NULL,
  "version" bigint NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "UNQ_mh_episode_asset_0" UNIQUE ("organization", "mediapackage", "mediapackageelement", "version"),
  CONSTRAINT "FK_mh_episode_asset_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE INDEX "IX_mh_episode_asset_mediapackage" ON "mh_episode_asset" ("mediapackage");
CREATE INDEX "IX_mh_episode_asset_checksum" ON "mh_episode_asset" ("checksum");
CREATE INDEX "IX_mh_episode_asset_uri" ON "mh_episode_asset" ("uri");

CREATE TABLE "mh_episode_version_claim" (
 "mediapackage" character varying(128) NOT NULL,
 "last_claimed" bigint NOT NULL,
 PRIMARY KEY ("mediapackage")
);

CREATE INDEX "IX_mh_episode_version_claim_mediapackage" ON "mh_episode_version_claim" ("mediapackage");
CREATE INDEX "IX_mh_episode_version_claim_last_claimed" ON "mh_episode_version_claim" ("last_claimed");



-- how to handle user & role? May be in & in 1.4 ddl but not in 1.3 ddl now mh_user and mh_role
CREATE TABLE IF NOT EXISTS "mh_user" (
  "username" character varying(128) NOT NULL,
  "organization" character varying(128) NOT NULL,
  "password" text,
  PRIMARY KEY ("username", "organization"),
  CONSTRAINT "FK_mh_user_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE TABLE IF NOT EXISTS "mh_role" (
  "username" character varying(128) NOT NULL,
  "organization" character varying(128) NOT NULL,
  "role" text,
  CONSTRAINT "FK_mh_role_username" FOREIGN KEY ("username", "organization") REFERENCES "mh_user" ("username", "organization"),
  CONSTRAINT "FK_mh_role_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);

CREATE INDEX "IX_mh_role_pk" ON "mh_role" ("username", "organization");


-- MH-8647
CREATE TABLE "mh_search" (
  "id" character varying(128) NOT NULL,
  "organization" character varying(128),
  "deletion_date" timestamp,
  "access_control" text,
  "mediapackage_xml" text,
  "modification_date" timestamp,
  PRIMARY KEY ("id"),
  CONSTRAINT "FK_mh_search_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id")
);





-- MH-8648 UCB Features
ALTER TABLE "mh_annotation" ADD COLUMN "private" boolean DEFAULT FALSE;
ALTER TABLE "mh_host_registration" ADD COLUMN "active" boolean DEFAULT TRUE;
ALTER TABLE "mh_service_registration" ADD COLUMN "active" boolean DEFAULT TRUE;
ALTER TABLE "mh_service_registration" ADD COLUMN "service_state" integer NOT NULL;
ALTER TABLE "mh_service_registration" ADD COLUMN "state_changed" timestamp;
ALTER TABLE "mh_service_registration" ADD COLUMN "warning_state_trigger" bigint;
ALTER TABLE "mh_service_registration" ADD COLUMN "error_state_trigger" bigint;
ALTER TABLE "mh_service_registration" ADD COLUMN "online_from" timestamp;



CREATE INDEX IX_mh_organization_node_pk ON mh_organization_node (organization);
CREATE INDEX IX_mh_organization_node_name ON mh_organization_node (name);
CREATE INDEX IX_mh_organization_node_port ON mh_organization_node (port);

CREATE TABLE mh_organization_property (
  organization VARCHAR(128) NOT NULL,
  name VARCHAR(255),
  value VARCHAR(255),
  PRIMARY KEY (organization, name),
  CONSTRAINT FK_mh_organization_property_organization FOREIGN KEY (organization) REFERENCES mh_organization (id)
);

CREATE INDEX IX_mh_organization_property_pk ON mh_organization_property (organization);




-- rename fields


ALTER TABLE "mh_annotation" RENAME COLUMN "mediapackage_id" TO "mediapackage";
ALTER TABLE "mh_annotation" RENAME COLUMN "session_id" TO "session";
ALTER TABLE "mh_annotation" RENAME COLUMN "user_id" TO "user";
ALTER TABLE "mh_annotation" RENAME COLUMN "annotation_type" TO "type";
ALTER TABLE "mh_annotation" RENAME COLUMN "annotation_value" TO "value";
-- ALTER TABLE "mh_annotation" RENAME COLUMN "private_annotation" TO "private";
ALTER TABLE "mh_job" RENAME COLUMN "parent_id" TO "parent";
ALTER TABLE "mh_job" RENAME COLUMN "root_id" TO "root";
ALTER TABLE "mh_job" RENAME COLUMN "is_dispatchable" TO "dispatchable";
ALTER TABLE "mh_job_argument" RENAME COLUMN "list_index" TO "argument_index";
ALTER TABLE "mh_job_context" RENAME COLUMN "root_job" TO "id";
ALTER TABLE "mh_job_context" RENAME COLUMN "key_entry" TO "name";
ALTER TABLE "mh_scheduled_event" RENAME COLUMN "event_id" TO "id";
ALTER TABLE "mh_series" RENAME COLUMN "series_id" TO "id";
ALTER TABLE "mh_series" RENAME COLUMN "organization_id" TO "organization";
ALTER TABLE "mh_user_action" RENAME COLUMN "mediapackage_id" TO "mediapackage";
ALTER TABLE "mh_user_action" RENAME COLUMN "session_id" TO "session";
ALTER TABLE "mh_user_action" RENAME COLUMN "user_id" TO "user";
ALTER TABLE "mh_user_action" RENAME COLUMN "is_playing" TO "playing";

-- add fields

ALTER TABLE "mh_annotation" ADD COLUMN "private" boolean DEFAULT FALSE;
ALTER TABLE "mh_host_registration" ADD COLUMN "active" boolean DEFAULT TRUE;
ALTER TABLE "mh_service_registration" ADD COLUMN "active" boolean DEFAULT TRUE;
ALTER TABLE "mh_service_registration" ADD COLUMN "service_state" integer NOT NULL;
ALTER TABLE "mh_service_registration" ADD COLUMN "state_changed" timestamp;
ALTER TABLE "mh_service_registration" ADD COLUMN "warning_state_trigger" bigint;
ALTER TABLE "mh_service_registration" ADD COLUMN "error_state_trigger" bigint;
ALTER TABLE "mh_service_registration" ADD COLUMN "online_from" timestamp;


-- add primary keys

ALTER TABLE "mh_capture_agent_role" ADD PRIMARY KEY ("id", "organization", "role");

-- add constraints

ALTER TABLE "mh_capture_agent_role" ADD CONSTRAINT "FK_mh_capture_agent_role_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id");
ALTER TABLE "mh_capture_agent_state" ADD CONSTRAINT "FK_mh_capture_agent_state_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id");
ALTER TABLE "mh_host_registration" ADD CONSTRAINT "UNQ_mh_host_registration_0" UNIQUE ("host");
ALTER TABLE "mh_service_registration" ADD CONSTRAINT "FK_service_registration_host_registration" FOREIGN KEY ("host_registration") REFERENCES "mh_host_registration" ("id");
ALTER TABLE "mh_job" ADD CONSTRAINT "FK_mh_job_creator_service" FOREIGN KEY ("creator_service") REFERENCES "mh_service_registration" ("id");
ALTER TABLE "mh_job" ADD CONSTRAINT "FK_mh_job_processor_service" FOREIGN KEY ("processor_service") REFERENCES "mh_service_registration" ("id");
ALTER TABLE "mh_job" ADD CONSTRAINT "FK_mh_job_parent" FOREIGN KEY ("parent") REFERENCES "mh_job" ("id");
ALTER TABLE "mh_job" ADD CONSTRAINT "FK_mh_job_root" FOREIGN KEY ("root") REFERENCES "mh_job" ("id");
ALTER TABLE "mh_job" ADD CONSTRAINT "FK_mh_job_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id");
ALTER TABLE "mh_job_argument" ADD CONSTRAINT "FK_mh_job_argument_id" FOREIGN KEY ("id") REFERENCES "mh_job" ("id");
ALTER TABLE "mh_job_context" ADD CONSTRAINT "FK_mh_job_context_id" FOREIGN KEY ("id") REFERENCES "mh_job" ("id");
ALTER TABLE "mh_user" ADD CONSTRAINT "FK_mh_user_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id");
ALTER TABLE "mh_role" ADD CONSTRAINT "FK_mh_role_username" FOREIGN KEY ("username", "organization") REFERENCES "mh_user" ("username", "organization");
ALTER TABLE "mh_role" ADD CONSTRAINT "FK_mh_role_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id");
ALTER TABLE "mh_series" ADD CONSTRAINT "FK_mh_series_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id");

-- add indices

CREATE INDEX "IX_mh_annotation_created" ON "mh_annotation" ("created");
CREATE INDEX "IX_mh_annotation_inpoint" ON "mh_annotation" ("inpoint");
CREATE INDEX "IX_mh_annotation_outpoint" ON "mh_annotation" ("outpoint");
CREATE INDEX "IX_mh_annotation_mediapackage" ON "mh_annotation" ("mediapackage");
CREATE INDEX "IX_mh_annotation_private" ON "mh_annotation" ("private");
CREATE INDEX "IX_mh_annotation_user" ON "mh_annotation" ("user");
CREATE INDEX "IX_mh_annotation_session" ON "mh_annotation" ("session");
CREATE INDEX "IX_mh_annotation_type" ON "mh_annotation" ("type");
CREATE INDEX "IX_mh_capture_agent_role" ON "mh_capture_agent_role" ("id", "organization");
CREATE INDEX "IX_mh_dictionary_weight" ON "mh_dictionary" ("weight");
CREATE INDEX "IX_mh_host_registration_online" ON "mh_host_registration" ("online");
CREATE INDEX "IX_mh_host_registration_active" ON "mh_host_registration" ("active");
CREATE INDEX "IX_mh_service_registration_service_type" ON "mh_service_registration" ("service_type");
CREATE INDEX "IX_mh_service_registration_service_state" ON "mh_service_registration" ("service_state");
CREATE INDEX "IX_mh_service_registration_active" ON "mh_service_registration" ("active");
CREATE INDEX "IX_mh_service_registration_host_registration" ON "mh_service_registration" ("host_registration");
CREATE INDEX "IX_mh_job_parent" ON "mh_job" ("parent");
CREATE INDEX "IX_mh_job_root" ON "mh_job" ("root");
CREATE INDEX "IX_mh_job_creator_service" ON "mh_job" ("creator_service");
CREATE INDEX "IX_mh_job_processor_service" ON "mh_job" ("processor_service");
CREATE INDEX "IX_mh_job_status" ON "mh_job" ("status");
CREATE INDEX "IX_mh_job_date_created" ON "mh_job" ("date_created");
CREATE INDEX "IX_mh_job_date_completed" ON "mh_job" ("date_completed");
CREATE INDEX "IX_mh_job_dispatchable" ON "mh_job" ("dispatchable");
CREATE INDEX "IX_mh_job_operation" ON "mh_job" ("operation");
CREATE INDEX "IX_mh_job_argument_id" ON "mh_job_argument" ("id");
CREATE INDEX "IX_mh_job_context_id" ON "mh_job_context" ("id");
CREATE INDEX "IX_mh_role_pk" ON "mh_role" ("username", "organization");
CREATE INDEX "IX_mh_user_action_created" ON "mh_user_action" ("created");
CREATE INDEX "IX_mh_user_action_inpoint" ON "mh_user_action" ("inpoint");
CREATE INDEX "IX_mh_user_action_outpoint" ON "mh_user_action" ("outpoint");
CREATE INDEX "IX_mh_user_action_mediapackage" ON "mh_user_action" ("mediapackage");
CREATE INDEX "IX_mh_user_action_user" ON "mh_user_action" ("user");
CREATE INDEX "IX_mh_user_action_session" ON "mh_user_action" ("session");
CREATE INDEX "IX_mh_user_action_type" ON "mh_user_action" ("type");



