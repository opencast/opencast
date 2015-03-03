CREATE TABLE IF NOT EXISTS mh_job_mh_service_registration (
  Job_id BIGINT NOT NULL,
  servicesRegistration_id BIGINT NOT NULL,
  PRIMARY KEY (Job_id, servicesRegistration_id),
  INDEX IX_mh_job_mh_service_registration_service_registration_id (servicesRegistration_id),
  CONSTRAINT FK_mh_job_mh_service_registration_Job_id FOREIGN KEY (Job_id) REFERENCES mh_job (id) ON DELETE CASCADE,
  CONSTRAINT mhjobmhservice_registrationservicesRegistration_id FOREIGN KEY (servicesRegistration_id) REFERENCES mh_service_registration (id) ON DELETE CASCADE
) ENGINE=InnoDB;

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
