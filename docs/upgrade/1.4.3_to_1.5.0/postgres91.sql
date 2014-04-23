DROP INDEX "IX_mh_role_pk";

ALTER TABLE mh_user rename to mh_user_tmp;
ALTER TABLE mh_role rename to mh_role_tmp;

CREATE TABLE "mh_role" (
  "id" bigint NOT NULL,
  "description" character varying(255) DEFAULT NULL,
  "name" character varying(128) DEFAULT NULL,
  "organization" character varying(128) DEFAULT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "UNQ_mh_role_0" UNIQUE ("name", "organization"),
  CONSTRAINT "FK_mh_role_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id") ON DELETE CASCADE
);

CREATE INDEX "IX_mh_role_pk" ON "mh_role" ("name", "organization");

-- Make mh_role temporary auto incrementable to fill it with the old mh_role table entries.
CREATE SEQUENCE seq START 1;

ALTER TABLE mh_role ALTER COLUMN "id" SET DEFAULT nextval('seq');
INSERT INTO mh_role (name, organization) SELECT role, organization FROM mh_role_tmp;
ALTER TABLE mh_role ALTER COLUMN "id" SET DEFAULT NULL;

DROP SEQUENCE seq;

CREATE TABLE "mh_user" (
  "id" bigint NOT NULL,
  "username" character varying(128) DEFAULT NULL,
  "password" text,
  "organization" character varying(128) DEFAULT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "UNQ_mh_user_0" UNIQUE ("username", "organization"),
  CONSTRAINT "FK_mh_user_organization" FOREIGN KEY ("organization") REFERENCES "mh_organization" ("id") ON DELETE CASCADE
);

-- Make mh_user temporary auto incrementable to fill it with the old mh_user table entries.
CREATE SEQUENCE seq START 1;

ALTER TABLE mh_user ALTER COLUMN "id" SET DEFAULT nextval('seq');
INSERT INTO mh_user (username, password, organization) SELECT username, password, organization FROM mh_user_tmp;
ALTER TABLE mh_user ALTER COLUMN "id" SET DEFAULT NULL;

DROP SEQUENCE seq;

CREATE TABLE mh_user_role (
  "user_id" bigint NOT NULL,
  "role_id" bigint NOT NULL,
  PRIMARY KEY ("user_id", "role_id"),
  CONSTRAINT "UNQ_mh_user_role_0" UNIQUE ("user_id", "role_id")
);

-- Create a temporary join table from the old mh_user and mh_role table.
CREATE TABLE mh_user_role_tmp AS (SELECT u.username, u.organization, role FROM mh_user_tmp AS u, mh_role_tmp AS r WHERE r.username = u.username AND r.organization = u.organization);

-- Fill the mh_user_role table by the temporary join table with the assigned users and roles from the new mh_user and mh_role table.
INSERT INTO mh_user_role (user_id, role_id) SELECT u.id, r.id FROM mh_user AS u, mh_role AS r, mh_user_role_tmp AS t WHERE u.username = t.username AND r.name = t.role;

-- Drop the temporary and old user and role table.
DROP TABLE mh_user_role_tmp;
DROP TABLE mh_role_tmp;
DROP TABLE mh_user_tmp;

CREATE TABLE mh_group (
  "id" bigint NOT NULL,
  "group_id" character varying(128) DEFAULT NULL,
  "description" character varying(255) DEFAULT NULL,
  "name" character varying(128) DEFAULT NULL,
  "role" character varying(255) DEFAULT NULL,
  "organization" character varying(128) DEFAULT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "UNQ_mh_group_0" UNIQUE ("group_id", "organization")
);

CREATE TABLE mh_group_member (
  "JpaGroup_id" bigint NOT NULL,
  "members" character varying(255) DEFAULT NULL
);

CREATE TABLE "mh_group_role" (
  "group_id" bigint NOT NULL,
  "role_id" bigint NOT NULL,
  PRIMARY KEY ("group_id", "role_id"),
  CONSTRAINT "UNQ_mh_group_role_0" UNIQUE ("group_id", "role_id")
);

CREATE TABLE "mh_acl_managed_acl" (
  "pk" bigint NOT NULL,
  "acl" text NOT NULL,
  "name" character varying(255) NOT NULL,
  "organization_id" character varying(255) NOT NULL,
  PRIMARY KEY ("pk"),
  CONSTRAINT "UNQ_mh_acl_managed_acl_0" UNIQUE ("name","organization_id")
);

CREATE TABLE "mh_acl_episode_transition" (
  "pk" bigint NOT NULL,
  "workflow_params" character varying(255) DEFAULT NULL,
  "application_date" timestamp DEFAULT NULL,
  "workflow_id" character varying(255) DEFAULT NULL,
  "done" boolean NOT NULL,
  "episode_id" character varying(128) DEFAULT NULL,
  "organization_id" character varying(128) DEFAULT NULL,
  "managed_acl_fk" bigint DEFAULT NULL,
  PRIMARY KEY ("pk"),
  CONSTRAINT "UNQ_mh_acl_episode_transition_0" UNIQUE ("episode_id","organization_id","application_date")
);

CREATE TABLE "mh_acl_series_transition" (
  "pk" bigint NOT NULL,
  "workflow_params" character varying(255) DEFAULT NULL,
  "application_date" timestamp DEFAULT NULL,
  "workflow_id" character varying(255) DEFAULT NULL,
  "override" boolean NOT NULL,
  "done" boolean NOT NULL,
  "organization_id" character varying(128) DEFAULT NULL,
  "series_id" character varying(128) DEFAULT NULL,
  "managed_acl_fk" bigint DEFAULT NULL,
  PRIMARY KEY ("pk"),
  CONSTRAINT "UNQ_mh_acl_series_transition_0" UNIQUE ("series_id","organization_id","application_date")
);

CREATE TABLE "mh_user_ref" (
  "id" bigint NOT NULL,
  "username" character varying(128) DEFAULT NULL,
  "last_login" timestamp DEFAULT NULL,
  "email" character varying(255) DEFAULT NULL,
  "name" character varying(255) DEFAULT NULL,
  "login_mechanism" character varying(255) DEFAULT NULL,
  "organization" character varying(128) DEFAULT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "UNQ_mh_user_ref_0" UNIQUE ("username", "organization")
);

CREATE TABLE mh_user_ref_role (
  "user_id" bigint NOT NULL,
  "role_id" bigint NOT NULL,
  PRIMARY KEY ("user_id", "role_id"),
  CONSTRAINT "UNQ_mh_user_ref_role_0" UNIQUE ("user_id", "role_id")
);