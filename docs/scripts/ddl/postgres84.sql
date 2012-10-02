--
-- Table: sequence
--
CREATE TABLE "sequence" (
  "seq_name" character varying(50) NOT NULL,
  "seq_count" numeric(38,0) DEFAULT NULL,
  PRIMARY KEY ("seq_name")
);

--
-- Table: annotation
--
CREATE TABLE "annotation" (
  "id" bigint NOT NULL,
  "outpoint" bigint DEFAULT NULL,
  "inpoint" bigint DEFAULT NULL,
  "mediapackage_id" character varying(36) DEFAULT NULL,
  "session_id" text,
  "created" timestamp DEFAULT NULL,
  "user_id" text,
  "length" bigint DEFAULT NULL,
  "annotation_value" text,
  "annotation_type" text,
  PRIMARY KEY ("id")
);

--
-- Table: capture_agent_role
--
CREATE TABLE "capture_agent_role" (
  "id" character varying(128) NOT NULL,
  "organization" character varying(128) NOT NULL,
  "role" character varying(255) DEFAULT NULL
);

--
-- Table: capture_agent_state
--
CREATE TABLE "capture_agent_state" (
  "organization" character varying(128) NOT NULL,
  "id" character varying(128) NOT NULL,
  "configuration" text,
  "state" text NOT NULL,
  "last_heard_from" bigint NOT NULL,
  "url" text,
  PRIMARY KEY ("organization", "id")
);

--
-- Table: dictionary
--
CREATE TABLE "dictionary" (
  "text" character varying(255) NOT NULL,
  "language" character varying(255) NOT NULL,
  "weight" numeric(8,2) DEFAULT NULL,
  "count" bigint DEFAULT NULL,
  "stop_word" boolean,
  PRIMARY KEY ("text", "language")
);

--
-- Table: host_registration
--
CREATE TABLE "host_registration" (
  "id" bigint NOT NULL,
  "host" character varying(255) NOT NULL,
  "maintenance" boolean NOT NULL,
  "max_jobs" bigint NOT NULL,
  "online" boolean NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "UNQ_host_registration_0" UNIQUE ("host")
);
CREATE INDEX "IX_host_registration_UNQ_host_registration_0" on "host_registration" ("host");

--
-- Table: job
--
CREATE TABLE "job" (
  "id" bigint NOT NULL,
  "status" bigint DEFAULT NULL,
  "payload" text,
  "date_started" timestamp DEFAULT NULL,
  "run_time" bigint DEFAULT NULL,
  "creator" text NOT NULL,
  "instance_version" bigint DEFAULT NULL,
  "date_completed" timestamp DEFAULT NULL,
  "operation" text,
  "is_dispatchable" boolean,
  "organization" text NOT NULL,
  "date_created" timestamp DEFAULT NULL,
  "queue_time" bigint DEFAULT NULL,
  "creator_service" bigint DEFAULT NULL,
  "parent_id" bigint DEFAULT NULL,
  "processor_service" bigint DEFAULT NULL,
  "root_id" bigint DEFAULT NULL,
  PRIMARY KEY ("id")
);
CREATE INDEX "FK_job_creator_service" on "job" ("creator_service");
CREATE INDEX "FK_job_parent_id" on "job" ("parent_id");
CREATE INDEX "FK_job_processor_service" on "job" ("processor_service");
CREATE INDEX "FK_job_root_id" on "job" ("root_id");

--
-- Table: job_arguments
--
CREATE TABLE "job_arguments" (
  "id" bigint NOT NULL,
  "argument" text,
  "list_index" bigint DEFAULT NULL,
  CONSTRAINT "UNQ_job_arguments_0" UNIQUE ("id", "list_index")
);
CREATE INDEX "IX_job_arguments_UNQ_job_arguments_0" on "job_arguments" ("id", "list_index");

--
-- Table: job_context
--
CREATE TABLE "job_context" (
  "root_job" bigint NOT NULL,
  "key_entry" character varying(255) NOT NULL,
  "value" text,
  CONSTRAINT "UNQ_job_context_0" UNIQUE ("root_job", "key_entry")
);

--
-- Table: matterhorn_role
--
CREATE TABLE "matterhorn_role" (
  "username" character varying(128) NOT NULL,
  "organization" character varying(128) NOT NULL,
  "role" text
);

--
-- Table: matterhorn_user
--
CREATE TABLE "matterhorn_user" (
  "username" character varying(128) NOT NULL,
  "organization" character varying(128) NOT NULL,
  "password" text,
  PRIMARY KEY ("username", "organization")
);

--
-- Table: scheduled_event
--
CREATE TABLE "scheduled_event" (
  "event_id" bigint NOT NULL,
  "capture_agent_metadata" text,
  "dublin_core" text,
  PRIMARY KEY ("event_id")
);

--
-- Table: search
--
CREATE TABLE "search" (
  "mediapackage_id" character varying(128) NOT NULL,
  "organization_id" character varying(128) DEFAULT NULL,
  "deletion_date" timestamp DEFAULT NULL,
  "access_control" text,
  "mediapackage" text,
  "modification_date" timestamp DEFAULT NULL,
  PRIMARY KEY ("mediapackage_id")
);

--
-- Table: series
--
CREATE TABLE "series" (
  "organization_id" character varying(128) NOT NULL,
  "series_id" character varying(128) NOT NULL,
  "access_control" text,
  "dublin_core" text,
  PRIMARY KEY ("organization_id", "series_id")
);

--
-- Table: service_registration
--
CREATE TABLE "service_registration" (
  "id" bigint NOT NULL,
  "path" text NOT NULL,
  "job_producer" boolean NOT NULL,
  "service_type" character varying(255) NOT NULL,
  "online" boolean NOT NULL,
  "online_from" timestamp,
  "service_state" character varying(32) NOT NULL,
  "state_changed" timestamp,
  "warning_state_trigger" bigint,
  "error_state_trigger" bigint,
  "host_registration" bigint DEFAULT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "UNQ_service_registration_0" UNIQUE ("host_registration", "service_type")
);
CREATE INDEX "IX_service_registration_UNQ_service_registration_0" on "service_registration" ("host_registration", "service_type");

--
-- Table: upload
--
CREATE TABLE "upload" (
  "id" character varying(255) NOT NULL,
  "total" bigint NOT NULL,
  "received" bigint NOT NULL,
  "filename" text NOT NULL,
  PRIMARY KEY ("id")
);

--
-- Table: user_action
--
CREATE TABLE "user_action" (
  "id" bigint NOT NULL,
  "user_ip" text,
  "outpoint" bigint DEFAULT NULL,
  "inpoint" bigint DEFAULT NULL,
  "mediapackage_id" text,
  "session_id" text,
  "created" timestamp DEFAULT NULL,
  "user_id" text,
  "length" bigint DEFAULT NULL,
  "type" text,
  "is_playing" boolean,
  PRIMARY KEY ("id")
);

CREATE TABLE "oaipmh_harvesting" (
	"url" text NOT NULL,
	"last_harvested" timestamp DEFAULT NULL,
	PRIMARY KEY (url)
);

--
-- Tables for episode service
--
CREATE TABLE "episode_asset" (
  "id" bigint NOT NULL,
  "mediapackageelement_id" character varying(255) NOT NULL,
  "mediapackage_id" character varying(255) NOT NULL,
  "organization_id" character varying(255) NOT NULL,
  "checksum" character varying(255) NOT NULL,
  "uri" character varying(255) NOT NULL,
  "version" bigint NOT NULL,
  PRIMARY KEY ("id")
);

CREATE TABLE "episode_episode" (
  "mediapackage_id" character varying(255) NOT NULL,
  "version" bigint NOT NULL,
  "latest_version" boolean NOT NULL,
  "organization_id" character varying(255) DEFAULT NULL,
  "deletion_date" timestamp DEFAULT NULL,
  "access_control" text,
  "locked" boolean NOT NULL,
  "mediapackage" text,
  "modification_date" timestamp DEFAULT NULL,
  PRIMARY KEY ("mediapackage_id", "version")
);

CREATE TABLE "episode_version_claim" (
 "media_package_id" character varying(255) NOT NULL,
 "last_claimed" bigint NOT NULL,
 PRIMARY KEY ("media_package_id")
);


INSERT INTO sequence (seq_name, seq_count) values ('SEQ_GEN',0);
