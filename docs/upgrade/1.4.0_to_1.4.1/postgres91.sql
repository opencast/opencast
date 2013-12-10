DROP INDEX "IX_mh_annotation_user";
DROP INDEX "IX_mh_user_action_user";

ALTER TABLE "mh_annotation"
RENAME COLUMN "user" TO "user_id";
ALTER TABLE "mh_user_action"
RENAME COLUMN "user" TO "user_id";

CREATE INDEX "IX_mh_annotation_user" ON "mh_annotation" ("user_id");
CREATE INDEX "IX_mh_user_action_user" ON "mh_user_action" ("user_id");

ALTER TABLE "mh_job_argument"
DROP CONSTRAINT IF EXISTS "UNQ_mh_job_argument_0";

ALTER TABLE "mh_annotation"
ALTER COLUMN "inpoint" TYPE integer,
ALTER COLUMN "outpoint" TYPE integer,
ALTER COLUMN "length" TYPE integer;

ALTER TABLE "mh_host_registration"
ALTER COLUMN "maintenance" SET DEFAULT FALSE,
ALTER COLUMN "max_jobs" TYPE integer;

ALTER TABLE "mh_service_registration"
ALTER COLUMN "job_producer" SET DEFAULT FALSE,
ALTER COLUMN "host_registration" DROP NOT NULL;

ALTER TABLE "mh_job_argument"
ALTER COLUMN "argument_index" TYPE integer;

ALTER TABLE "mh_user_action"
ALTER COLUMN "user_ip" TYPE character varying(255),
ALTER COLUMN "inpoint" TYPE integer,
ALTER COLUMN "outpoint" TYPE integer,
ALTER COLUMN "length" TYPE integer;
