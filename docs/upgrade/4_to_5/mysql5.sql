UPDATE SEQUENCE SET SEQ_NAME = REPLACE(SEQ_NAME, 'seq_mh_assets_asset', 'seq_oc_assets_asset');
UPDATE SEQUENCE SET SEQ_NAME = REPLACE(SEQ_NAME, 'seq_mh_assets_snapshot', 'seq_oc_assets_snapshot');

RENAME TABLE mh_bundleinfo TO oc_bundleinfo;
ALTER TABLE oc_bundleinfo ADD CONSTRAINT UNQ_oc_bundleinfo UNIQUE (host, bundle_name, bundle_version);
ALTER TABLE oc_bundleinfo DROP INDEX IF EXISTS UNQ_mh_bundleinfo;

RENAME TABLE mh_organization TO oc_organization;

RENAME TABLE mh_organization_node TO oc_organization_node;
ALTER TABLE oc_organization_node ADD CONSTRAINT FK_oc_organization_node_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_organization_node DROP FOREIGN KEY FK_mh_organization_node_organization;

CREATE INDEX IX_oc_organization_node_pk ON oc_organization_node (organization);
DROP INDEX IF EXISTS IX_mh_organization_node_pk ON oc_organization_node;
CREATE INDEX IX_oc_organization_node_name ON oc_organization_node (name);
DROP INDEX IF EXISTS IX_mh_organization_node_name ON oc_organization_node;
CREATE INDEX IX_oc_organization_node_port ON oc_organization_node (port);
DROP INDEX IF EXISTS IX_mh_organization_node_port ON oc_organization_node;

RENAME TABLE mh_organization_property TO oc_organization_property;
ALTER TABLE oc_organization_property ADD CONSTRAINT FK_oc_organization_property_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_organization_property DROP FOREIGN KEY FK_mh_organization_property_organization;

CREATE INDEX IX_oc_organization_property_pk ON oc_organization_property (organization);
DROP INDEX IF EXISTS IX_mh_organization_property_pk ON oc_organization_property;

RENAME TABLE mh_annotation TO oc_annotation;

CREATE INDEX IX_oc_annotation_created ON oc_annotation (created);
DROP INDEX IF EXISTS IX_mh_annotation_created ON oc_annotation;
CREATE INDEX IX_oc_annotation_inpoint ON oc_annotation (inpoint);
DROP INDEX IF EXISTS IX_mh_annotation_inpoint ON oc_annotation;
CREATE INDEX IX_oc_annotation_outpoint ON oc_annotation (outpoint);
DROP INDEX IF EXISTS IX_mh_annotation_outpoint ON oc_annotation;
CREATE INDEX IX_oc_annotation_mediapackage ON oc_annotation (mediapackage);
DROP INDEX IF EXISTS IX_mh_annotation_mediapackage ON oc_annotation;
CREATE INDEX IX_oc_annotation_private ON oc_annotation (private);
DROP INDEX IF EXISTS IX_mh_annotation_private ON oc_annotation;
CREATE INDEX IX_oc_annotation_user ON oc_annotation (user_id);
DROP INDEX IF EXISTS IX_mh_annotation_user ON oc_annotation;
CREATE INDEX IX_oc_annotation_session ON oc_annotation (session);
DROP INDEX IF EXISTS IX_mh_annotation_session ON oc_annotation;
CREATE INDEX IX_oc_annotation_type ON oc_annotation (type);
DROP INDEX IF EXISTS IX_mh_annotation_type ON oc_annotation;

RENAME TABLE mh_capture_agent_role TO oc_capture_agent_role;
ALTER TABLE oc_capture_agent_role ADD CONSTRAINT FK_oc_capture_agent_role_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_capture_agent_role DROP FOREIGN KEY FK_mh_capture_agent_role_organization;

CREATE INDEX IX_oc_capture_agent_role_pk ON oc_capture_agent_role (id, organization);
DROP INDEX IF EXISTS IX_mh_capture_agent_role_pk ON oc_capture_agent_role;

RENAME TABLE mh_capture_agent_state TO oc_capture_agent_state;
ALTER TABLE oc_capture_agent_state ADD CONSTRAINT FK_oc_capture_agent_state_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_capture_agent_state DROP FOREIGN KEY FK_mh_capture_agent_state_organization;

RENAME TABLE mh_host_registration TO oc_host_registration;
ALTER TABLE oc_host_registration ADD CONSTRAINT UNQ_oc_host_registration UNIQUE (host);
ALTER TABLE oc_host_registration DROP INDEX IF EXISTS UNQ_mh_host_registration;

CREATE INDEX IX_oc_host_registration_online ON oc_host_registration (online);
DROP INDEX IF EXISTS IX_mh_host_registration_online ON oc_host_registration;
CREATE INDEX IX_oc_host_registration_active ON oc_host_registration (active);
DROP INDEX IF EXISTS IX_mh_host_registration_active ON oc_host_registration;

RENAME TABLE mh_service_registration TO oc_service_registration;
ALTER TABLE oc_service_registration ADD CONSTRAINT UNQ_oc_service_registration UNIQUE (host_registration, service_type);
ALTER TABLE oc_service_registration DROP INDEX IF EXISTS UNQ_mh_service_registration;
ALTER TABLE oc_service_registration ADD CONSTRAINT FK_oc_service_registration_host_registration FOREIGN KEY (host_registration) REFERENCES oc_host_registration (id) ON DELETE CASCADE;
ALTER TABLE oc_service_registration DROP FOREIGN KEY FK_mh_service_registration_host_registration;

CREATE INDEX IX_oc_service_registration_service_type ON oc_service_registration (service_type);
DROP INDEX IF EXISTS IX_mh_service_registration_service_type ON oc_service_registration;
CREATE INDEX IX_oc_service_registration_service_state ON oc_service_registration (service_state);
DROP INDEX IF EXISTS IX_mh_service_registration_service_state ON oc_service_registration;
CREATE INDEX IX_oc_service_registration_active ON oc_service_registration (active);
DROP INDEX IF EXISTS IX_mh_service_registration_active ON oc_service_registration;
CREATE INDEX IX_oc_service_registration_host_registration ON oc_service_registration (host_registration);
DROP INDEX IF EXISTS IX_mh_service_registration_host_registration ON oc_service_registration;

RENAME TABLE mh_job TO oc_job;
ALTER TABLE oc_job ADD CONSTRAINT FK_oc_job_creator_service FOREIGN KEY (creator_service) REFERENCES oc_service_registration (id) ON DELETE CASCADE;
ALTER TABLE oc_job DROP FOREIGN KEY FK_mh_job_creator_service;
ALTER TABLE oc_job ADD CONSTRAINT FK_oc_job_processor_service FOREIGN KEY (processor_service) REFERENCES oc_service_registration (id) ON DELETE CASCADE;
ALTER TABLE oc_job DROP FOREIGN KEY FK_mh_job_processor_service;
ALTER TABLE oc_job ADD CONSTRAINT FK_oc_job_parent FOREIGN KEY (parent) REFERENCES oc_job (id) ON DELETE CASCADE;
ALTER TABLE oc_job DROP FOREIGN KEY FK_mh_job_parent;
ALTER TABLE oc_job ADD CONSTRAINT FK_oc_job_root FOREIGN KEY (root) REFERENCES oc_job (id) ON DELETE CASCADE;
ALTER TABLE oc_job DROP FOREIGN KEY FK_mh_job_root;
ALTER TABLE oc_job ADD CONSTRAINT FK_oc_job_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_job DROP FOREIGN KEY FK_mh_job_organization;

CREATE INDEX IX_oc_job_parent ON oc_job (parent);
DROP INDEX IF EXISTS IX_mh_job_parent ON oc_job;
CREATE INDEX IX_oc_job_root ON oc_job (root);
DROP INDEX IF EXISTS IX_mh_job_root ON oc_job;
CREATE INDEX IX_oc_job_creator_service ON oc_job (creator_service);
DROP INDEX IF EXISTS IX_mh_job_creator_service ON oc_job;
CREATE INDEX IX_oc_job_processor_service ON oc_job (processor_service);
DROP INDEX IF EXISTS IX_mh_job_processor_service ON oc_job;
CREATE INDEX IX_oc_job_status ON oc_job (status);
DROP INDEX IF EXISTS IX_mh_job_status ON oc_job;
CREATE INDEX IX_oc_job_date_created ON oc_job (date_created);
DROP INDEX IF EXISTS IX_mh_job_date_created ON oc_job;
CREATE INDEX IX_oc_job_date_completed ON oc_job (date_completed);
DROP INDEX IF EXISTS IX_mh_job_date_completed ON oc_job;
CREATE INDEX IX_oc_job_dispatchable ON oc_job (dispatchable);
DROP INDEX IF EXISTS IX_mh_job_dispatchable ON oc_job;
CREATE INDEX IX_oc_job_operation ON oc_job (operation);
DROP INDEX IF EXISTS IX_mh_job_operation ON oc_job;
CREATE INDEX IX_oc_job_statistics ON oc_job (processor_service, status, queue_time, run_time);
DROP INDEX IF EXISTS IX_mh_job_statistics ON oc_job;

RENAME TABLE mh_job_argument TO oc_job_argument;
ALTER TABLE oc_job_argument ADD CONSTRAINT FK_oc_job_argument_id FOREIGN KEY (id) REFERENCES oc_job (id) ON DELETE CASCADE;
ALTER TABLE oc_job_argument DROP FOREIGN KEY FK_mh_job_argument_id;

CREATE INDEX IX_oc_job_argument_id ON oc_job_argument (id);
DROP INDEX IF EXISTS IX_mh_job_argument_id ON oc_job_argument;

RENAME TABLE mh_blocking_job TO oc_blocking_job;
/* Note the constraint name is changing subtly here FK_blocking_job_id to FK*_oc*_blocking_job_id */
ALTER TABLE oc_blocking_job ADD CONSTRAINT FK_oc_blocking_job_id FOREIGN KEY (id) REFERENCES oc_job (id) ON DELETE CASCADE;
ALTER TABLE oc_blocking_job DROP FOREIGN KEY FK_blocking_job_id;

RENAME TABLE mh_job_context TO oc_job_context;
ALTER TABLE oc_job_context ADD CONSTRAINT UNQ_oc_job_context UNIQUE (id, name);
ALTER TABLE oc_job_context DROP INDEX IF EXISTS UNQ_mh_job_context;
ALTER TABLE oc_job_context ADD CONSTRAINT FK_oc_job_context_id FOREIGN KEY (id) REFERENCES oc_job (id) ON DELETE CASCADE;
ALTER TABLE oc_job_context DROP FOREIGN KEY FK_mh_job_context_id;

CREATE INDEX IX_oc_job_context_id ON oc_job_context (id);
DROP INDEX IF EXISTS IX_mh_job_context_id ON oc_job_context;

RENAME TABLE mh_job_mh_service_registration TO oc_job_oc_service_registration;
ALTER TABLE oc_job_oc_service_registration ADD CONSTRAINT FK_oc_job_oc_service_registration_Job_id FOREIGN KEY (Job_id) REFERENCES oc_job (id) ON DELETE CASCADE;
ALTER TABLE oc_job_oc_service_registration DROP FOREIGN KEY FK_mh_job_mh_service_registration_Job_id;
ALTER TABLE oc_job_oc_service_registration ADD CONSTRAINT FK_oc_job_oc_service_registration_servicesRegistration_id FOREIGN KEY (servicesRegistration_id) REFERENCES oc_service_registration (id) ON DELETE CASCADE;
ALTER TABLE oc_job_oc_service_registration DROP FOREIGN KEY FK_mh_job_mh_service_registration_servicesRegistration_id;

CREATE INDEX IX_oc_job_oc_service_registration_servicesRegistration_id ON oc_job_oc_service_registration (servicesRegistration_id);
DROP INDEX IF EXISTS IX_mh_job_mh_service_registration_servicesRegistration_id ON oc_job_oc_service_registration;

RENAME TABLE mh_incident TO oc_incident;
ALTER TABLE oc_incident ADD CONSTRAINT FK_oc_incident_jobid FOREIGN KEY (jobid) REFERENCES oc_job (id) ON DELETE CASCADE;
ALTER TABLE oc_incident DROP FOREIGN KEY FK_mh_incident_jobid;

CREATE INDEX IX_oc_incident_jobid ON oc_incident (jobid);
DROP INDEX IF EXISTS IX_mh_incident_jobid ON oc_incident;
CREATE INDEX IX_oc_incident_severity ON oc_incident (severity);
DROP INDEX IF EXISTS IX_mh_incident_severity ON oc_incident;

RENAME TABLE mh_incident_text TO oc_incident_text;

RENAME TABLE mh_scheduled_last_modified TO oc_scheduled_last_modified;

CREATE INDEX IX_oc_scheduled_last_modified_last_modified ON oc_scheduled_last_modified (last_modified);
DROP INDEX IF EXISTS IX_mh_scheduled_last_modified_last_modified ON oc_scheduled_last_modified;

RENAME TABLE mh_scheduled_extended_event TO oc_scheduled_extended_event;
ALTER TABLE oc_scheduled_extended_event ADD CONSTRAINT FK_oc_scheduled_extended_event_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_scheduled_extended_event DROP FOREIGN KEY FK_mh_scheduled_extended_event_organization;

RENAME TABLE mh_scheduled_transaction TO oc_scheduled_transaction;
ALTER TABLE oc_scheduled_transaction ADD CONSTRAINT UNQ_oc_scheduled_transaction UNIQUE (id, organization, source);
ALTER TABLE oc_scheduled_transaction DROP INDEX IF EXISTS UNQ_mh_scheduled_transaction;
ALTER TABLE oc_scheduled_transaction ADD CONSTRAINT FK_oc_scheduled_transaction_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_scheduled_transaction DROP FOREIGN KEY FK_mh_scheduled_transaction_organization;

CREATE INDEX IX_oc_scheduled_transaction_source ON oc_scheduled_transaction (source);
DROP INDEX IF EXISTS IX_mh_scheduled_transaction_source ON oc_scheduled_transaction;

RENAME TABLE mh_search TO oc_search;
ALTER TABLE oc_search ADD CONSTRAINT FK_oc_search_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_search DROP FOREIGN KEY FK_mh_search_organization;

CREATE INDEX IX_oc_search_organization ON oc_search (organization);
DROP INDEX IF EXISTS IX_mh_search_organization ON oc_search;

RENAME TABLE mh_series TO oc_series;
ALTER TABLE oc_series ADD CONSTRAINT FK_oc_series_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_series DROP FOREIGN KEY FK_mh_series_organization;

RENAME TABLE mh_oaipmh TO oc_oaipmh;
ALTER TABLE oc_oaipmh ADD CONSTRAINT UNQ_oc_oaipmh UNIQUE (modification_date);
ALTER TABLE oc_oaipmh DROP INDEX IF EXISTS UNQ_mh_oaipmh;

CREATE INDEX IX_oc_oaipmh_modification_date ON oc_oaipmh (modification_date);
DROP INDEX IF EXISTS IX_mh_oaipmh_modification_date ON oc_oaipmh;

DROP TRIGGER mh_init_oaipmh_date;
CREATE TRIGGER oc_init_oaipmh_date BEFORE INSERT ON `oc_oaipmh`
FOR EACH ROW SET NEW.modification_date = NOW();

DROP TRIGGER mh_update_oaipmh_date;
CREATE TRIGGER oc_update_oaipmh_date BEFORE UPDATE ON `oc_oaipmh`
FOR EACH ROW SET NEW.modification_date = NOW();

RENAME TABLE mh_oaipmh_elements TO oc_oaipmh_elements;
ALTER TABLE oc_oaipmh_elements ADD CONSTRAINT FK_oc_oaipmh_elements 
    FOREIGN KEY (mp_id, repo_id, organization)
    REFERENCES oc_oaipmh (mp_id, repo_id, organization)
    ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE oc_oaipmh_elements DROP FOREIGN KEY FK_mh_oaipmh_elements;

RENAME TABLE mh_user_session TO oc_user_session;

CREATE INDEX IX_oc_user_session_user_id ON oc_user_session (user_id);
DROP INDEX IF EXISTS IX_mh_user_session_user_id ON oc_user_session;

RENAME TABLE mh_user_action TO oc_user_action;
ALTER TABLE oc_user_action ADD CONSTRAINT FK_oc_user_action_session_id FOREIGN KEY (session_id) REFERENCES oc_user_session (session_id) ON DELETE CASCADE;
ALTER TABLE oc_user_action DROP FOREIGN KEY FK_mh_user_action_session_id;

CREATE INDEX IX_oc_user_action_created ON oc_user_action (created);
DROP INDEX IF EXISTS IX_mh_user_action_created ON oc_user_action;
CREATE INDEX IX_oc_user_action_inpoint ON oc_user_action (inpoint);
DROP INDEX IF EXISTS IX_mh_user_action_inpoint ON oc_user_action;
CREATE INDEX IX_oc_user_action_outpoint ON oc_user_action (outpoint);
DROP INDEX IF EXISTS IX_mh_user_action_outpoint ON oc_user_action;
CREATE INDEX IX_oc_user_action_mediapackage_id ON oc_user_action (mediapackage);
DROP INDEX IF EXISTS IX_mh_user_action_mediapackage_id ON oc_user_action;
CREATE INDEX IX_oc_user_action_type ON oc_user_action (type);
DROP INDEX IF EXISTS IX_mh_user_action_type ON oc_user_action;

RENAME TABLE mh_oaipmh_harvesting TO oc_oaipmh_harvesting;

RENAME TABLE mh_assets_snapshot TO oc_assets_snapshot;
ALTER TABLE oc_assets_snapshot ADD CONSTRAINT UNQ_oc_assets_snapshot  UNIQUE (mediapackage_id, version);
ALTER TABLE oc_assets_snapshot DROP INDEX IF EXISTS UNQ_mh_assets_snapshot;
ALTER TABLE oc_assets_snapshot ADD CONSTRAINT FK_oc_assets_snapshot_organization FOREIGN KEY (organization_id) REFERENCES oc_organization (id);
ALTER TABLE oc_assets_snapshot DROP FOREIGN KEY FK_mh_assets_snapshot_organization;
ALTER TABLE oc_assets_snapshot ADD COLUMN storage_id VARCHAR(256) NOT NULL;
CREATE INDEX IX_oc_assets_snapshot_archival_date ON oc_assets_snapshot (archival_date);
DROP INDEX IF EXISTS IX_mh_assets_snapshot_archival_date ON oc_assets_snapshot;
CREATE INDEX IX_oc_assets_snapshot_mediapackage_id ON oc_assets_snapshot (mediapackage_id);
DROP INDEX IF EXISTS IX_mh_assets_snapshot_mediapackage_id ON oc_assets_snapshot;
CREATE INDEX IX_oc_assets_snapshot_organization_id ON oc_assets_snapshot (organization_id);
DROP INDEX IF EXISTS IX_mh_assets_snapshot_organization_id ON oc_assets_snapshot;
CREATE INDEX IX_oc_assets_snapshot_owner ON oc_assets_snapshot (owner);
DROP INDEX IF EXISTS IX_mh_assets_snapshot_owner ON oc_assets_snapshot;

RENAME TABLE mh_assets_asset TO oc_assets_asset;
CREATE INDEX IX_oc_assets_asset_checksum ON oc_assets_asset (checksum);
DROP INDEX IF EXISTS IX_mh_assets_asset_checksum ON oc_assets_asset;
CREATE INDEX IX_oc_assets_asset_mediapackage_element_id ON oc_assets_asset (mediapackage_element_id);
DROP INDEX IF EXISTS IX_mh_assets_asset_mediapackage_element_id ON oc_assets_asset;

RENAME TABLE mh_assets_properties TO oc_assets_properties;
CREATE INDEX IX_oc_assets_properties_val_date ON oc_assets_properties (val_date);
DROP INDEX IF EXISTS IX_mh_assets_properties_val_date on oc_assets_properties;
CREATE INDEX IX_oc_assets_properties_val_long ON oc_assets_properties  (val_long);
DROP INDEX IF EXISTS IX_mh_assets_properties_val_long on oc_assets_properties;
CREATE INDEX IX_oc_assets_properties_val_string ON oc_assets_properties  (val_string);
DROP INDEX IF EXISTS IX_mh_assets_properties_val_string on oc_assets_properties;
CREATE INDEX IX_oc_assets_properties_val_bool ON oc_assets_properties  (val_bool);
DROP INDEX IF EXISTS IX_mh_assets_properties_val_bool on oc_assets_properties;
CREATE INDEX IX_oc_assets_properties_mediapackage_id ON oc_assets_properties  (mediapackage_id);
DROP INDEX IF EXISTS IX_mh_assets_properties_mediapackage_id on oc_assets_properties;
CREATE INDEX IX_oc_assets_properties_namespace ON oc_assets_properties  (namespace);
DROP INDEX IF EXISTS IX_mh_assets_properties_namespace on oc_assets_properties;
CREATE INDEX IX_oc_assets_properties_property_name ON oc_assets_properties  (property_name);
DROP INDEX IF EXISTS IX_mh_assets_properties_property_name on oc_assets_properties;

RENAME TABLE mh_assets_version_claim TO oc_assets_version_claim;

RENAME TABLE mh_acl_managed_acl TO oc_acl_managed_acl;
ALTER TABLE oc_acl_managed_acl ADD CONSTRAINT UNQ_oc_acl_managed_acl UNIQUE (name, organization_id);
ALTER TABLE oc_acl_managed_acl DROP INDEX IF EXISTS UNQ_mh_acl_managed_acl;

RENAME TABLE mh_acl_episode_transition TO oc_acl_episode_transition;
ALTER TABLE oc_acl_episode_transition ADD CONSTRAINT UNQ_oc_acl_episode_transition UNIQUE (episode_id, organization_id, application_date);
ALTER TABLE oc_acl_episode_transition DROP INDEX IF EXISTS UNQ_mh_acl_episode_transition;
ALTER TABLE oc_acl_episode_transition ADD CONSTRAINT FK_oc_acl_episode_transition_managed_acl_fk FOREIGN KEY (managed_acl_fk) REFERENCES oc_acl_managed_acl (pk);
ALTER TABLE oc_acl_episode_transition DROP FOREIGN KEY FK_mh_acl_episode_transition_managed_acl_fk;

RENAME TABLE mh_acl_series_transition TO oc_acl_series_transition;
ALTER TABLE oc_acl_series_transition ADD CONSTRAINT UNQ_oc_acl_series_transition UNIQUE (series_id, organization_id, application_date);
ALTER TABLE oc_acl_series_transition DROP INDEX IF EXISTS  UNQ_mh_acl_series_transition;
ALTER TABLE oc_acl_series_transition ADD CONSTRAINT FK_oc_acl_series_transition_managed_acl_fk FOREIGN KEY (managed_acl_fk) REFERENCES oc_acl_managed_acl (pk);
ALTER TABLE oc_acl_series_transition DROP FOREIGN KEY FK_mh_acl_series_transition_managed_acl_fk;

RENAME TABLE mh_role TO oc_role;
ALTER TABLE oc_role ADD CONSTRAINT UNQ_oc_role UNIQUE (name, organization);
ALTER TABLE oc_role DROP INDEX IF EXISTS UNQ_mh_role;
ALTER TABLE oc_role ADD CONSTRAINT FK_oc_role_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_role DROP FOREIGN KEY FK_mh_role_organization;

RENAME TABLE mh_group TO oc_group;
ALTER TABLE oc_group ADD CONSTRAINT UNQ_oc_group UNIQUE (group_id, organization);
ALTER TABLE oc_group DROP INDEX IF EXISTS UNQ_mh_group;
ALTER TABLE oc_group ADD CONSTRAINT FK_oc_group_organization FOREIGN KEY (organization) REFERENCES oc_organization (id);
ALTER TABLE oc_group DROP FOREIGN KEY FK_mh_group_organization;

RENAME TABLE mh_group_member TO oc_group_member;

RENAME TABLE mh_group_role TO oc_group_role;
ALTER TABLE oc_group_role ADD CONSTRAINT UNQ_oc_group_role UNIQUE (group_id, role_id);
ALTER TABLE oc_group_role DROP INDEX IF EXISTS UNQ_mh_group_role;
ALTER TABLE oc_group_role ADD CONSTRAINT FK_oc_group_role_group_id FOREIGN KEY (group_id) REFERENCES oc_group (id);
ALTER TABLE oc_group_role DROP FOREIGN KEY FK_mh_group_role_group_id;
ALTER TABLE oc_group_role ADD CONSTRAINT FK_oc_group_role_role_id FOREIGN KEY (role_id) REFERENCES oc_role (id);
ALTER TABLE oc_group_role DROP FOREIGN KEY FK_mh_group_role_role_id;

RENAME TABLE mh_user TO oc_user;
ALTER TABLE oc_user ADD CONSTRAINT UNQ_oc_user UNIQUE (username, organization);
ALTER TABLE oc_user DROP INDEX IF EXISTS UNQ_mh_user;
ALTER TABLE oc_user ADD CONSTRAINT FK_oc_user_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_user DROP FOREIGN KEY FK_mh_user_organization;

CREATE INDEX IX_oc_role_pk ON oc_role (name, organization);
DROP INDEX IF EXISTS IX_mh_role_pk ON oc_role;

RENAME TABLE mh_user_role TO oc_user_role;
ALTER TABLE oc_user_role ADD CONSTRAINT UNQ_oc_user_role UNIQUE (user_id, role_id);
ALTER TABLE oc_user_role DROP INDEX IF EXISTS UNQ_mh_user_role;
ALTER TABLE oc_user_role ADD CONSTRAINT FK_oc_user_role_role_id FOREIGN KEY (role_id) REFERENCES oc_role (id);
ALTER TABLE oc_user_role DROP FOREIGN KEY FK_mh_user_role_role_id;
ALTER TABLE oc_user_role ADD CONSTRAINT FK_oc_user_role_user_id FOREIGN KEY (user_id) REFERENCES oc_user (id);
ALTER TABLE oc_user_role DROP FOREIGN KEY FK_mh_user_role_user_id;

RENAME TABLE mh_user_ref TO oc_user_ref;
ALTER TABLE oc_user_ref ADD CONSTRAINT UNQ_oc_user_ref UNIQUE (username, organization);
ALTER TABLE oc_user_ref DROP INDEX IF EXISTS UNQ_mh_user_ref;
ALTER TABLE oc_user_ref ADD CONSTRAINT FK_oc_user_ref_organization FOREIGN KEY (organization) REFERENCES oc_organization (id);
ALTER TABLE oc_user_ref DROP FOREIGN KEY FK_mh_user_ref_organization;

RENAME TABLE mh_user_ref_role TO oc_user_ref_role;
DROP INDEX IF EXISTS UNQ_mh_user_ref_role ON oc_user_ref_role;
ALTER TABLE oc_user_ref_role ADD CONSTRAINT FK_oc_user_ref_role_role_id FOREIGN KEY (role_id) REFERENCES oc_role (id);
ALTER TABLE oc_user_ref_role DROP FOREIGN KEY FK_mh_user_ref_role_role_id;
ALTER TABLE oc_user_ref_role ADD CONSTRAINT FK_oc_user_ref_role_user_id FOREIGN KEY (user_id) REFERENCES oc_user_ref (id);
ALTER TABLE oc_user_ref_role DROP FOREIGN KEY FK_mh_user_ref_role_user_id;

RENAME TABLE mh_user_settings TO oc_user_settings;
ALTER TABLE oc_user_settings ADD CONSTRAINT UNQ_oc_user_settings UNIQUE (username, organization);
ALTER TABLE oc_user_settings DROP INDEX IF EXISTS UNQ_mh_user_settings;

CREATE INDEX IX_oc_user_setting_organization ON oc_user_settings (organization);
DROP INDEX IF EXISTS IX_mh_user_setting_organization ON oc_user_settings;

RENAME TABLE mh_email_configuration TO oc_email_configuration;
ALTER TABLE oc_email_configuration ADD CONSTRAINT UNQ_oc_email_configuration UNIQUE (organization);
ALTER TABLE oc_email_configuration DROP INDEX IF EXISTS UNQ_mh_email_configuration;
ALTER TABLE oc_email_configuration ADD CONSTRAINT FK_oc_email_configuration_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_email_configuration DROP FOREIGN KEY FK_mh_email_configuration_organization;

CREATE INDEX IX_oc_email_configuration_organization ON oc_email_configuration (organization);
DROP INDEX IF EXISTS IX_mh_email_configuration_organization ON oc_email_configuration;

RENAME TABLE mh_message_signature TO oc_message_signature;
ALTER TABLE oc_message_signature ADD CONSTRAINT UNQ_oc_message_signature UNIQUE (organization, name);
ALTER TABLE oc_message_signature DROP INDEX IF EXISTS UNQ_mh_message_signature;
ALTER TABLE oc_message_signature ADD CONSTRAINT FK_oc_message_signature_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_message_signature DROP FOREIGN KEY FK_mh_message_signature_organization;

CREATE INDEX IX_oc_message_signature_organization ON oc_message_signature (organization);
DROP INDEX IF EXISTS IX_mh_message_signature_organization ON oc_message_signature;
CREATE INDEX IX_oc_message_signature_name ON oc_message_signature (name);
DROP INDEX IF EXISTS IX_mh_message_signature_name ON oc_message_signature;

RENAME TABLE mh_message_template TO oc_message_template;
ALTER TABLE oc_message_template ADD CONSTRAINT UNQ_oc_message_template UNIQUE (organization, name);
ALTER TABLE oc_message_template DROP INDEX IF EXISTS UNQ_mh_message_template;
ALTER TABLE oc_message_template ADD CONSTRAINT FK_oc_message_template_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_message_template DROP FOREIGN KEY FK_mh_message_template_organization;

CREATE INDEX IX_oc_message_template_organization ON oc_message_template (organization);
DROP INDEX IF EXISTS IX_mh_message_template_organization ON oc_message_template;
CREATE INDEX IX_oc_message_template_name ON oc_message_template (name);
DROP INDEX IF EXISTS IX_mh_message_template_name ON oc_message_template;

RENAME TABLE mh_event_comment TO oc_event_comment;

RENAME TABLE mh_event_comment_reply TO oc_event_comment_reply;
ALTER TABLE oc_event_comment_reply ADD CONSTRAINT FK_oc_event_comment_reply_oc_event_comment FOREIGN KEY (event_comment_id) REFERENCES oc_event_comment (id) ON DELETE CASCADE;
ALTER TABLE oc_event_comment_reply DROP FOREIGN KEY FK_mh_event_comment_reply_mh_event_comment;

RENAME TABLE mh_series_elements TO oc_series_elements;

RENAME TABLE mh_series_property TO oc_series_property;
ALTER TABLE oc_series_property ADD CONSTRAINT FK_oc_series_property_organization_series FOREIGN KEY (organization, series) REFERENCES oc_series (organization, id) ON DELETE CASCADE;
ALTER TABLE oc_series_property DROP FOREIGN KEY FK_mh_series_property_organization_series;

CREATE INDEX IX_oc_series_property_pk ON oc_series_property (series);
DROP INDEX IF EXISTS IX_mh_series_property_pk ON oc_series_property;

RENAME TABLE mh_themes TO oc_themes;
ALTER TABLE oc_themes ADD CONSTRAINT FK_oc_themes_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE;
ALTER TABLE oc_themes DROP FOREIGN KEY FK_mh_themes_organization;

RENAME TABLE mh_ibm_watson_transcript_job TO oc_ibm_watson_transcript_job;
