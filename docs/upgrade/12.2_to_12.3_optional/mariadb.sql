CREATE INDEX IF NOT EXISTS IX_oc_job_argument_id ON oc_job_argument (id);
CREATE INDEX IF NOT EXISTS IX_oc_job_argument_id_argument ON oc_job_argument (id, argument);

ALTER TABLE oc_workflow
  MODIFY COLUMN `description` LONGTEXT DEFAULT NULL;

ALTER TABLE oc_workflow_operation
  MODIFY COLUMN `description` LONGTEXT DEFAULT NULL,
  MODIFY COLUMN `if_condition` VARCHAR(65535) DEFAULT NULL;
