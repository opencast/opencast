CREATE INDEX IF NOT EXISTS IX_oc_job_argument_id ON oc_job_argument (id);
CREATE INDEX IF NOT EXISTS IX_oc_job_argument_id_argument ON oc_job_argument (id, argument);

ALTER TABLE oc_workflow
  ALTER COLUMN description TYPE TEXT;

ALTER TABLE oc_workflow_operation
  ALTER COLUMN description TYPE TEXT,
  ALTER COLUMN if_condition TYPE VARCHAR(65535);
