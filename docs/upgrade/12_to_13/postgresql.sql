CREATE INDEX IF NOT EXISTS IX_oc_aws_asset_mapping_object_key ON oc_aws_asset_mapping (object_key);
CREATE INDEX IF NOT EXISTS IX_oc_job_argument_id ON oc_job_argument (id);

ALTER TABLE oc_workflow
  ALTER COLUMN description TYPE TEXT;

ALTER TABLE oc_workflow_operation
  ALTER COLUMN description TYPE TEXT,
  ALTER COLUMN if_condition TYPE TEXT;
