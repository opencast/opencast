set @exist := (SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'mh_job_mh_service_registration');
set @sqlstmt := if( @exist > 0, 'SELECT "mh_job_mh_service_registration table already existing"', 'CREATE TABLE mh_job_mh_service_registration (
  Job_id BIGINT NOT NULL,
  servicesRegistration_id BIGINT NOT NULL,
  PRIMARY KEY (Job_id, servicesRegistration_id),
  KEY mhjobmhservice_registrationservicesRegistration_id (servicesRegistration_id),
  CONSTRAINT FK_mh_job_mh_service_registration_Job_id FOREIGN KEY (Job_id) REFERENCES mh_job (id) ON DELETE CASCADE,
  CONSTRAINT mhjobmhservice_registrationservicesRegistration_id FOREIGN KEY (servicesRegistration_id) REFERENCES mh_service_registration (id) ON DELETE CASCADE
) ENGINE=InnoDB;' );
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

CREATE TABLE mh_incident (
  id BIGINT NOT NULL,
  jobid BIGINT,
  timestamp DATETIME,
  code VARCHAR(255),
  severity INTEGER,
  parameters TEXT(65535),
  details TEXT(65535),
  PRIMARY KEY (id),
  CONSTRAINT FK_job_incident_jobid FOREIGN KEY (jobid) REFERENCES mh_job (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX IX_mh_incident_jobid ON mh_incident (jobid);
CREATE INDEX IX_mh_incident_severity ON mh_incident (severity);

CREATE TABLE mh_incident_text (
  id VARCHAR(255) NOT NULL,
  text VARCHAR(2038) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
