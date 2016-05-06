ALTER TABLE mh_job ADD job_load FLOAT NOT NULL DEFAULT '1.0';
ALTER TABLE mh_job ADD blocking_job BIGINT(20) DEFAULT NULL;

ALTER TABLE mh_host_registration ADD max_load FLOAT NOT NULL DEFAULT '1.0';
UPDATE mh_host_registration SET max_load=max_jobs;
ALTER TABLE mh_host_registration DROP max_jobs;

CREATE TABLE mh_blocking_job (
  id BIGINT NOT NULL,
  blocking_job_list BIGINT,
  job_index INTEGER,
  CONSTRAINT FK_blocking_job_id FOREIGN KEY (id) REFERENCES mh_job (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE mh_user ADD COLUMN manageable TINYINT(1) NOT NULL DEFAULT '1';
