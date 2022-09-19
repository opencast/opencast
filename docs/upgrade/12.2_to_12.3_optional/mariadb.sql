CREATE INDEX IF NOT EXISTS IX_oc_job_argument_id ON oc_job_argument (id);
CREATE INDEX IF NOT EXISTS IX_oc_job_argument_id_argument ON oc_job_argument (id, argument);

ALTER TABLE oc_workflow
  MODIFY COLUMN `description` LONGTEXT DEFAULT NULL;

ALTER TABLE oc_workflow_operation
  MODIFY COLUMN `description` LONGTEXT DEFAULT NULL,
  MODIFY COLUMN `if_condition` VARCHAR(65535) DEFAULT NULL;

-- This requires MariaDB 10.5.2 and newer
ALTER TABLE oc_workflow_configuration
  RENAME INDEX FK_oc_workflow_configuration_workflow_id TO IX_oc_workflow_configuration_workflow_id;

ALTER TABLE oc_workflow_operation
  RENAME INDEX FK_oc_workflow_operation_workflow_id TO IX_oc_workflow_operation_workflow_id;

ALTER TABLE oc_workflow_operation_configuration
  RENAME INDEX cworkflowoperationconfigurationworkflowoperationid TO IX_oc_workflow_operation_configuration_workflow_operation_id;
