
drop schema if exists matterhorn;
create schema matterhorn DEFAULT CHARACTER SET = 'utf8';
use matterhorn;

CREATE TABLE annotation (
	id BIGINT NOT NULL,
	outpoint INTEGER,
	inpoint INTEGER,
	mediapackage_id VARCHAR(36),
	session_id TEXT(65535),
	created DATETIME,
	user_id TEXT(65535),
	length INTEGER,
	annotation_value TEXT(65535),
	annotation_type TEXT(65535),
	PRIMARY KEY (
	id)
);

CREATE TABLE capture_agent_role (
	id VARCHAR(255) NOT NULL,
	organization VARCHAR(255) NOT NULL,
	role VARCHAR(255)
);

CREATE TABLE capture_agent_state (
	organization VARCHAR(255) NOT NULL,
	id VARCHAR(255) NOT NULL,
	configuration TEXT(65535),
	state TEXT(65535) NOT NULL,
	last_heard_from BIGINT NOT NULL,
	url TEXT(65535),
	PRIMARY KEY (
	organization,
	id)
);

CREATE TABLE dictionary (
	text VARCHAR(255) NOT NULL,
	language VARCHAR(255) NOT NULL,
	weight DOUBLE,
	count BIGINT,
	stop_word TINYINT(1) default 0,
	PRIMARY KEY (
	text,
	language)
);

CREATE TABLE host_registration (
	id BIGINT NOT NULL,
	host VARCHAR(255) NOT NULL,
	maintenance TINYINT(1) default 0 NOT NULL,
	max_jobs INTEGER NOT NULL,
	online TINYINT(1) default 0 NOT NULL,
	PRIMARY KEY (
	id)
);

CREATE TABLE job_arguments (
	id BIGINT NOT NULL,
	argument TEXT(2147483647),
	list_index INTEGER
);

CREATE TABLE job (
	id BIGINT NOT NULL,
	status INTEGER,
	payload TEXT(65535),
	date_started DATETIME,
	run_time BIGINT,
	creator TEXT(65535) NOT NULL,
	instance_version BIGINT,
	date_completed DATETIME,
	operation TEXT(65535),
	is_dispatchable TINYINT(1) default 0,
	organization TEXT(65535) NOT NULL,
	date_created DATETIME,
	queue_time BIGINT,
	creator_service BIGINT,
	parent_id BIGINT,
	processor_service BIGINT,
	root_id BIGINT,
	PRIMARY KEY (
	id)
);

CREATE TABLE matterhorn_role (
	username VARCHAR(255) NOT NULL,
	organization VARCHAR(255) NOT NULL,
	role TEXT(65535)
);

CREATE TABLE matterhorn_user (
	username VARCHAR(255) NOT NULL,
	organization VARCHAR(255) NOT NULL,
	password TEXT(65535),
	PRIMARY KEY (
	username,
	organization)
);

CREATE TABLE scheduled_event (
	event_id BIGINT NOT NULL,
	capture_agent_metadata TEXT(65535),
	dublin_core TEXT(65535),
	PRIMARY KEY (
	event_id)
);

CREATE TABLE SEQUENCE (
	SEQ_NAME VARCHAR(50) NOT NULL,
	SEQ_COUNT DECIMAL(38),
	PRIMARY KEY (
	SEQ_NAME)
);

CREATE TABLE series (
	organization_id VARCHAR(128) NOT NULL,
	series_id VARCHAR(128) NOT NULL,
	access_control TEXT(65535),
	dublin_core TEXT(65535),
	PRIMARY KEY (
	organization_id,
	series_id)
);

CREATE TABLE service_registration (
	id BIGINT NOT NULL,
	job_producer TINYINT(1) default 0 NOT NULL,
	path TEXT(65535) NOT NULL,
	service_type VARCHAR(255) NOT NULL,
	online TINYINT(1) default 0 NOT NULL,
	host_registration BIGINT,
	PRIMARY KEY (
	id)
);

CREATE TABLE upload (
	id VARCHAR(255) NOT NULL,
	total BIGINT NOT NULL,
	received BIGINT NOT NULL,
	filename TEXT(65535) NOT NULL,
	PRIMARY KEY (
	id)
);

CREATE TABLE user_action (
	id BIGINT NOT NULL,
	user_ip TEXT(65535),
	outpoint INTEGER,
	inpoint INTEGER,
	mediapackage_id TEXT(65535),
	session_id TEXT(65535),
	created DATETIME,
	user_id TEXT(65535),
	length INTEGER,
	type TEXT(65535),
	is_playing TINYINT(1) default 0,
	PRIMARY KEY (
	id)
);
ALTER TABLE host_registration ADD CONSTRAINT UNQ_host_registration_0 UNIQUE (
	host
);

ALTER TABLE job ADD CONSTRAINT FK_job_creator_service FOREIGN KEY (
	creator_service) REFERENCES service_registration (
	id
);

ALTER TABLE job ADD CONSTRAINT FK_job_parent_id FOREIGN KEY (
	parent_id) REFERENCES job (
	id
);

ALTER TABLE job ADD CONSTRAINT FK_job_processor_service FOREIGN KEY (
	processor_service) REFERENCES service_registration (
	id
);

ALTER TABLE job ADD CONSTRAINT FK_job_root_id FOREIGN KEY (
	root_id) REFERENCES job (
	id
);

ALTER TABLE job_arguments ADD CONSTRAINT UNQ_job_arguments_0 UNIQUE (
	id,
	list_index
);

ALTER TABLE service_registration ADD CONSTRAINT FK_service_registration_host_registration FOREIGN KEY (
	host_registration) REFERENCES host_registration (
	id
);

ALTER TABLE service_registration ADD CONSTRAINT UNQ_service_registration_0 UNIQUE (
	host_registration,
	service_type
);
CREATE INDEX IX_host_registration_UNQ_host_registration_0 ON host_registration (
	host
);

CREATE INDEX IX_job_arguments_UNQ_job_arguments_0 ON job_arguments (
	id,
	list_index
);

CREATE INDEX IX_service_registration_UNQ_service_registration_0 ON service_registration (
	host_registration,
	service_type
);
INSERT INTO SEQUENCE(SEQ_NAME,
	SEQ_COUNT) values (
	'SEQ_GEN',
	0
);
