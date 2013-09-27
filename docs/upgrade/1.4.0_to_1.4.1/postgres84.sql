DROP INDEX "IX_mh_annotation_user" ON "mh_annotation";
DROP INDEX "IX_mh_user_action_user" ON "mh_user_action";

ALTER TABLE mh_annotation CHANGE user user_id VARCHAR(255);
ALTER TABLE mh_user_action CHANGE user user_id VARCHAR(255);

CREATE INDEX "IX_mh_annotation_user_id" ON "mh_annotation" ("user_id");
CREATE INDEX "IX_mh_user_action_user_id" ON "mh_user_action" ("user_id");

DROP INDEX IF EXISTS "UNQ_job_argument_0" ON "mh_job_argument";
