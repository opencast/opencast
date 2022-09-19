CREATE INDEX IF NOT EXISTS IX_oc_aws_asset_mapping_object_key ON oc_aws_asset_mapping (object_key);
CREATE INDEX IF NOT EXISTS IX_oc_job_argument_id ON oc_job_argument (id);

ALTER TABLE oc_workflow
  ALTER COLUMN description TYPE TEXT;

ALTER TABLE oc_workflow_operation
  ALTER COLUMN description TYPE TEXT,
  ALTER COLUMN if_condition TYPE TEXT;

CREATE INDEX IF NOT EXISTS IX_oc_workflow_configuration_workflow_id ON oc_workflow_configuration (workflow_id);
CREATE INDEX IF NOT EXISTS IX_oc_workflow_operation_workflow_id ON oc_workflow_operation (workflow_id);
CREATE INDEX IF NOT EXISTS IX_oc_workflow_operation_configuration_workflow_operation_id ON oc_workflow_operation_configuration (workflow_operation_id);
