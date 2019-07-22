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

-- MH-12047 Add series index for efficiency
CREATE INDEX IX_oc_search_series ON oc_search (series_id);

-- MH-13380 Add snapshot_id index for efficiency
CREATE INDEX IX_oc_assets_asset_snapshot_id ON oc_assets_asset (snapshot_id);

-- MH-13490 Add event index for efficiency
CREATE INDEX IX_oc_event_comment_event ON oc_event_comment (event, organization);

-- MH-13489 Add index on series_id for efficiency
CREATE INDEX IX_oc_assets_snapshot_series ON oc_assets_snapshot (series_id, version);
