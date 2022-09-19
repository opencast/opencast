CREATE INDEX IF NOT EXISTS IX_oc_job_argument_id ON oc_job_argument (id);
CREATE INDEX IF NOT EXISTS IX_oc_job_argument_id_argument ON oc_job_argument (id, argument);

ALTER TABLE oc_workflow
  ALTER COLUMN description TYPE TEXT;

ALTER TABLE oc_workflow_operation
  ALTER COLUMN description TYPE TEXT,
  ALTER COLUMN if_condition TYPE VARCHAR(65535);

ALTER INDEX IF EXISTS ix_oc_workflow_mediapackage_id RENAME TO IX_oc_workflow_mediapackage_id;
ALTER INDEX IF EXISTS ix_oc_workflow_series_id RENAME TO IX_oc_workflow_series_id;
CREATE INDEX IF NOT EXISTS IX_oc_workflow_configuration_workflow_id ON oc_workflow_configuration (workflow_id);
CREATE INDEX IF NOT EXISTS IX_workflow_operation_table_name_workflow_id ON workflow_operation_table_name (workflow_id);
CREATE INDEX IF NOT EXISTS IX_workflow_operation_configuration_table_name_workflow_operation_id ON workflow_operation_configuration_table_name (workflow_operation_id);
