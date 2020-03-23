-- Due to MH-13514 Add descriptive node names to hosts
ALTER TABLE oc_host_registration ADD COLUMN node_name VARCHAR(255) AFTER host;

ALTER TABLE oc_aws_asset_mapping CHANGE COLUMN media_package_element mediapackage_element varchar(128);
ALTER TABLE oc_aws_asset_mapping CHANGE COLUMN media_package mediapackage varchar(128);


-- Create provider table for transcription service
CREATE TABLE oc_transcription_service_provider (
  id BIGINT(20) NOT NULL,
  provider VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Create table for transcription service job with link to transcription provider table
CREATE TABLE oc_transcription_service_job (
  id BIGINT(20) NOT NULL,
  mediapackage_id VARCHAR(128) NOT NULL,
  track_id VARCHAR(128) NOT NULL,
  job_id  VARCHAR(128) NOT NULL,
  date_created DATETIME NOT NULL,
  date_expected DATETIME DEFAULT NULL,
  date_completed DATETIME DEFAULT NULL,
  status VARCHAR(128) DEFAULT NULL,
  track_duration BIGINT NOT NULL,
  provider_id BIGINT(20) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FK_oc_transcription_service_job_provider_id FOREIGN KEY (provider_id) REFERENCES oc_transcription_service_provider (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Migrate IBM Watson transcription data to new shared table

INSERT INTO oc_transcription_service_provider (id, provider) VALUES (1, "IBM Watson");

INSERT INTO oc_transcription_service_job (id, mediapackage_id, track_id, job_id, date_created, date_completed, status, track_duration, provider_id)
 (SELECT id, media_package_id, track_id, job_id, date_created, date_completed, status, track_duration, 1 FROM oc_ibm_watson_transcript_job);

DROP TABLE oc_ibm_watson_transcript_job;
