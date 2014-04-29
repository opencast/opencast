CREATE TABLE mh_job_mh_service_registration (
  Job_id BIGINT NOT NULL,
  servicesRegistration_id BIGINT NOT NULL,
  PRIMARY KEY (Job_id, servicesRegistration_id),
  KEY mhjobmhservice_registrationservicesRegistration_id (servicesRegistration_id),
  CONSTRAINT FK_mh_job_mh_service_registration_Job_id FOREIGN KEY (Job_id) REFERENCES mh_job (id) ON DELETE CASCADE,
  CONSTRAINT mhjobmhservice_registrationservicesRegistration_id FOREIGN KEY (servicesRegistration_id) REFERENCES mh_service_registration (id) ON DELETE CASCADE
) ENGINE=InnoDB;
