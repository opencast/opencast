ALTER TABLE oc_assets_asset ADD CONSTRAINT FK_oc_assets_asset_snapshot_id FOREIGN KEY (snapshot_id) REFERENCES oc_assets_snapshot (id) ON DELETE CASCADE;
DROP TABLE oc_scheduled_transaction;
DROP TABLE oc_scheduled_extended_event;
CREATE TABLE oc_scheduled_extended_event (
  mediapackage_id VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  capture_agent_id VARCHAR(128) NOT NULL,
  start_date DATETIME NOT NULL,
  end_date DATETIME NOT NULL,
  source VARCHAR(255),
  recording_state VARCHAR(255),
  recording_last_heard BIGINT,
  presenters TEXT(65535),
  last_modified_date DATETIME,
  checksum VARCHAR(64),
  capture_agent_properties MEDIUMTEXT,
  workflow_properties MEDIUMTEXT,
  PRIMARY KEY (mediapackage_id, organization),
  CONSTRAINT FK_oc_scheduled_extended_event_organization FOREIGN KEY (organization) REFERENCES oc_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX IX_oc_scheduled_extended_event_organization ON oc_scheduled_extended_event (organization);
CREATE INDEX IX_oc_scheduled_extended_event_capture_agent_id ON oc_scheduled_extended_event (capture_agent_id);
CREATE INDEX IX_oc_scheduled_extended_event_dates ON oc_scheduled_extended_event (start_date, end_date);
DELETE FROM oc_assets_properties WHERE namespace = 'org.opencastproject.scheduler.trx';

ALTER TABLE oc_job DROP COLUMN blocking_job;
DROP TABLE oc_blocking_job;

-- Due to MH-13397 Remove unfinished feature "Participation Management"
ALTER TABLE oc_series DROP COLUMN opt_out;

-- Due to MH-13431 Remove unfinished feature "Bulk Messaging"
DROP TABLE oc_email_configuration;
DROP TABLE oc_message_signature;
DROP TABLE oc_message_template;

-- Due to MH-13446 Remove unfinished feature "ACL transitions"
DROP TABLE oc_acl_episode_transition;
DROP TABLE oc_acl_series_transition;

-- Clean up orphaned asset manager properties
delete p from oc_assets_properties p where not exists (
  select * from oc_assets_snapshot s
    where p.mediapackage_id = s.mediapackage_id
);

CREATE TABLE oc_transcription_service_provider (
  id BIGINT(20) NOT NULL,
  provider VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE oc_transcription_service_job (
  id BIGINT(20) NOT NULL,
  media_package_id VARCHAR(128) NOT NULL,
  track_id VARCHAR(128) NOT NULL,
  job_id  VARCHAR(128) NOT NULL,
  date_created DATETIME NOT NULL,
  date_completed DATETIME DEFAULT NULL,
  status VARCHAR(128) DEFAULT NULL,
  track_duration BIGINT NOT NULL,
  provider_id BIGINT(20) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FK_oc_transcription_service_job_provider_id FOREIGN KEY (provider_id) REFERENCES oc_transcription_service_provider (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
