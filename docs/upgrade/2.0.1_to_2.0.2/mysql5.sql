-- Consistency updates MH-10588, MH-11067 --

ALTER TABLE mh_user_action
DROP FOREIGN KEY FK_mh_user_action_session_id;
ALTER TABLE mh_user_action
ADD CONSTRAINT FK_mh_user_action_session_id FOREIGN KEY (session_id) REFERENCES mh_user_session (session_id) ON DELETE CASCADE;

ALTER TABLE mh_host_registration
DROP INDEX UNQ_mh_host_registration_0;
ALTER TABLE mh_host_registration
ADD CONSTRAINT UNQ_mh_host_registration UNIQUE (host);

ALTER TABLE mh_service_registration
DROP INDEX UNQ_mh_service_registration_0;
ALTER TABLE mh_service_registration
ADD CONSTRAINT UNQ_mh_service_registration UNIQUE (host_registration, service_type);

ALTER TABLE mh_service_registration
DROP FOREIGN KEY FK_service_registration_host_registration;
ALTER TABLE mh_service_registration
ADD CONSTRAINT FK_mh_service_registration_host_registration FOREIGN KEY (host_registration) REFERENCES mh_host_registration (id) ON DELETE CASCADE;

ALTER TABLE mh_job_argument
DROP FOREIGN KEY FK_job_argument_id;
ALTER TABLE mh_job_argument
ADD CONSTRAINT FK_mh_job_argument_id FOREIGN KEY (id) REFERENCES mh_job (id) ON DELETE CASCADE;

ALTER TABLE mh_job_context
DROP INDEX UNQ_job_context_0;
ALTER TABLE mh_job_context
ADD CONSTRAINT UNQ_mh_job_context UNIQUE (id, name);

ALTER TABLE mh_job_context
DROP FOREIGN KEY FK_job_context_id;
ALTER TABLE mh_job_context
ADD CONSTRAINT FK_mh_job_context_id FOREIGN KEY (id) REFERENCES mh_job (id) ON DELETE CASCADE;

ALTER TABLE mh_job_mh_service_registration
DROP FOREIGN KEY mhjobmhservice_registrationservicesRegistration_id;
ALTER TABLE mh_job_mh_service_registration
ADD CONSTRAINT FK_mh_job_mh_service_registration_servicesRegistration_id FOREIGN KEY (servicesRegistration_id) REFERENCES mh_service_registration (id) ON DELETE CASCADE;

ALTER TABLE mh_incident
DROP FOREIGN KEY FK_job_incident_jobid;
ALTER TABLE mh_incident
ADD CONSTRAINT FK_mh_incident_jobid FOREIGN KEY (jobid) REFERENCES mh_job (id) ON DELETE CASCADE;

ALTER TABLE mh_acl_managed_acl
DROP INDEX UNQ_mh_acl_managed_acl_0;
ALTER TABLE mh_acl_managed_acl
ADD CONSTRAINT UNQ_mh_acl_managed_acl UNIQUE (name, organization_id);

ALTER TABLE mh_acl_episode_transition
DROP INDEX UNQ_mh_acl_episode_transition_0;
ALTER TABLE mh_acl_episode_transition
ADD CONSTRAINT UNQ_mh_acl_episode_transition UNIQUE (episode_id, organization_id, application_date);

ALTER TABLE mh_acl_series_transition
DROP INDEX UNQ_mh_acl_series_transition_0;
ALTER TABLE mh_acl_series_transition
ADD CONSTRAINT UNQ_mh_acl_series_transition UNIQUE (series_id, organization_id, application_date);

ALTER TABLE mh_role
DROP INDEX UNQ_mh_role_0;
ALTER TABLE mh_role
ADD CONSTRAINT UNQ_mh_role UNIQUE (name, organization);

ALTER TABLE mh_group
DROP INDEX UNQ_mh_group_0;
ALTER TABLE mh_group
ADD CONSTRAINT UNQ_mh_group UNIQUE (group_id, organization);

ALTER TABLE mh_group_role
DROP INDEX UNQ_mh_group_role_0;
ALTER TABLE mh_group_role
ADD CONSTRAINT UNQ_mh_group_role UNIQUE (group_id, role_id);

ALTER TABLE mh_group_member
CHANGE JpaGroup_id group_id bigint(20) NOT NULL,
CHANGE MEMBERS member varchar(255) DEFAULT NULL;

ALTER TABLE mh_user_settings
DROP FOREIGN KEY FK_mh_user_setting_username;

ALTER TABLE mh_user
DROP INDEX UNQ_mh_user_0;
ALTER TABLE mh_user
ADD CONSTRAINT UNQ_mh_user UNIQUE (username, organization);

ALTER TABLE mh_user_settings
ADD CONSTRAINT FK_mh_user_setting_username FOREIGN KEY (username) REFERENCES mh_user (username);

ALTER TABLE mh_user_role
DROP INDEX UNQ_mh_user_role_0;
ALTER TABLE mh_user_role
ADD CONSTRAINT UNQ_mh_user_role UNIQUE (user_id, role_id);

ALTER TABLE mh_user_ref
DROP INDEX UNQ_mh_user_ref_0;
ALTER TABLE mh_user_ref
ADD CONSTRAINT UNQ_mh_user_ref UNIQUE (username, organization);

ALTER TABLE mh_user_ref_role
DROP INDEX UNQ_mh_user_ref_role_0;
ALTER TABLE mh_user_ref_role
ADD CONSTRAINT UNQ_mh_user_ref_role UNIQUE (user_id, role_id);

-- Fix naming conventions from 2.0.1

ALTER TABLE mh_series_property
DROP FOREIGN KEY FK_mh_series_property_series;
ALTER TABLE mh_series_property
ADD CONSTRAINT FK_mh_series_property_organization_series FOREIGN KEY (organization, series) REFERENCES mh_series (organization, id) ON DELETE CASCADE;

ALTER TABLE mh_email_configuration
DROP INDEX UNQ_mh_email_configuration_0;
ALTER TABLE mh_email_configuration
ADD CONSTRAINT UNQ_mh_email_configuration UNIQUE (organization);

ALTER TABLE mh_comment_mh_comment_reply
DROP INDEX FK_mh_comment_mh_comment_reply_replies_id,
DROP FOREIGN KEY FK_mh_comment_mh_comment_reply_replies_id,
DROP FOREIGN KEY FK_mh_comment_mh_comment_reply_Comment_id;
ALTER TABLE mh_comment_mh_comment_reply
DROP PRIMARY KEY,
CHANGE Comment_id comment_id BIGINT(20) NOT NULL,
ADD PRIMARY KEY (comment_id,replies_id),
ADD CONSTRAINT FK_mh_comment_mh_comment_reply_comment_id FOREIGN KEY (comment_id) REFERENCES mh_comment (id),
ADD CONSTRAINT FK_mh_comment_mh_comment_reply_replies_id FOREIGN KEY (replies_id) REFERENCES mh_comment_reply (id);

CREATE INDEX IX_mh_comment_mh_comment_reply_replies on mh_comment_mh_comment_reply (replies_id);

ALTER TABLE mh_message_signature
DROP INDEX UNQ_mh_message_signature_0;
ALTER TABLE mh_message_signature
ADD CONSTRAINT UNQ_mh_message_signature UNIQUE (organization, name);

ALTER TABLE mh_message_signature_mh_comment
DROP KEY mh_message_signature_mh_comment_comments_id,
DROP FOREIGN KEY mh_message_signature_mh_comment_comments_id,
DROP FOREIGN KEY mhmessagesignaturemhcommentMessageSignature_id;
ALTER TABLE mh_message_signature_mh_comment
DROP PRIMARY KEY;
ALTER TABLE mh_message_signature_mh_comment
CHANGE MessageSignature_id message_signature_id BIGINT(20) NOT NULL,
ADD PRIMARY KEY (message_signature_id, comments_id),
ADD CONSTRAINT FK_mh_message_signature_mh_comment_comments_id FOREIGN KEY (comments_id) REFERENCES mh_comment (id) ON DELETE CASCADE,
ADD CONSTRAINT FK_mh_message_signature_mh_comment_message_signature_id FOREIGN KEY (message_signature_id) REFERENCES mh_message_signature (id) ON DELETE CASCADE;

ALTER TABLE mh_message_template
CHANGE TYPE template_type VARCHAR(255) DEFAULT NULL,
DROP INDEX UNQ_mh_message_template_0;
ALTER TABLE mh_message_template
ADD CONSTRAINT UNQ_mh_message_template UNIQUE (organization, name);

ALTER TABLE mh_message_template_mh_comment
DROP KEY FK_mh_message_template_mh_comment_comments_id,
DROP FOREIGN KEY mhmessagetemplatemh_commentMessageTemplate_id,
DROP FOREIGN KEY mh_message_template_mh_comment_comments_id;
ALTER TABLE mh_message_template_mh_comment
DROP PRIMARY KEY,
CHANGE MessageTemplate_id message_template_id BIGINT(20) NOT NULL,
ADD PRIMARY KEY (message_template_id, comments_id),
ADD CONSTRAINT FK_mh_message_template_mh_comment_message_template_id FOREIGN KEY (message_template_id) REFERENCES mh_message_template (id) ON DELETE CASCADE,
ADD CONSTRAINT FK_mh_message_template_mh_comment_comments_id FOREIGN KEY (comments_id) REFERENCES mh_comment (id) ON DELETE CASCADE;

ALTER TABLE mh_archive_asset
DROP INDEX UNQ_mh_archive_asset_0,
ADD CONSTRAINT UNQ_mh_archive_asset UNIQUE (organization, mediapackage, mediapackageelement, version);

ALTER TABLE mh_archive_episode
DROP INDEX IX_mh_archive_episode_mediapackage;
ALTER TABLE mh_archive_episode
ADD INDEX IX_mh_archive_episode_id (id);

CREATE INDEX IX_mh_archive_episode_organization on mh_archive_episode (organization);
CREATE INDEX IX_mh_event_mh_comment_comment on mh_event_mh_comment (comment);

