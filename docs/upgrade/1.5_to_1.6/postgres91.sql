CREATE TABLE "mh_incident" (
  "id" bigint NOT NULL,
  "jobid" bigint,
  "timestamp" timestamp,
  "code" character varying(255),
  "severity" integer,
  "parameters" text,
  "details" text,
  PRIMARY KEY ("id"),
  CONSTRAINT "FK_job_incident_jobid" FOREIGN KEY ("jobid") REFERENCES "mh_job" ("id") ON DELETE CASCADE
);

CREATE INDEX "IX_mh_incident_jobid" ON "mh_incident" ("jobid");
CREATE INDEX "IX_mh_incident_severity" ON "mh_incident" ("severity");

CREATE TABLE "mh_incident_text" (
  "id" character varying (255) NOT NULL,
  text character varying(2038) NOT NULL,
  PRIMARY KEY (id)
);