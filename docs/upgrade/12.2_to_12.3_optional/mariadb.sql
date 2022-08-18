CREATE INDEX IF NOT EXISTS IX_oc_job_argument_id ON oc_job_argument (id);
CREATE INDEX IF NOT EXISTS IX_oc_job_argument_id_argument ON oc_job_argument (id, argument);
