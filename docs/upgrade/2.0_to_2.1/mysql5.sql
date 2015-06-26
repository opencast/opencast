ALTER TABLE mh_job ADD job_load FLOAT NOT NULL DEFAULT '1.0';

ALTER TABLE mh_host_registration ADD max_load FLOAT NOT NULL DEFAULT '1.0';
UPDATE mh_host_registration SET max_load=max_jobs;
ALTER TABLE mh_host_registration DROP max_jobs;
