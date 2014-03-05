ALTER TABLE mh_user rename to mh_user_tmp;
ALTER TABLE mh_role rename to mh_role_tmp;

CREATE TABLE mh_role (
  id bigint(20) NOT NULL,
  description varchar(255) DEFAULT NULL,
  name varchar(128) DEFAULT NULL,
  organization varchar(128) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_role_0 UNIQUE (name, organization),
  CONSTRAINT FK_mh_role_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX IX_mh_role_pk ON mh_role (name, organization);

-- Make mh_role temporary auto incrementable to fill it with the old mh_role table entries.
ALTER TABLE mh_role MODIFY id bigint(20) NOT NULL AUTO_INCREMENT;
INSERT INTO mh_role (name, organization) SELECT role, organization FROM mh_role_tmp GROUP BY role;
ALTER TABLE mh_role MODIFY id bigint(20) NOT NULL;

CREATE TABLE mh_user (
  id bigint(20) NOT NULL,
  username varchar(128) DEFAULT NULL,
  password text,
  organization varchar(128) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_user_0 UNIQUE (username, organization),
  CONSTRAINT FK_mh_user_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Make mh_user temporary auto incrementable to fill it with the old mh_user table entries.
ALTER TABLE mh_user MODIFY id bigint(20) NOT NULL AUTO_INCREMENT;
INSERT INTO mh_user (username, password, organization) SELECT username, password, organization FROM mh_user_tmp;
ALTER TABLE mh_user MODIFY id bigint(20) NOT NULL;

CREATE TABLE mh_user_role (
  user_id bigint(20) NOT NULL,
  role_id bigint(20) NOT NULL,
  PRIMARY KEY (user_id,role_id),
  CONSTRAINT UNQ_mh_user_role_0 UNIQUE (user_id, role_id),
  CONSTRAINT FK_mh_user_role_role_id FOREIGN KEY (role_id) REFERENCES mh_role (id),
  CONSTRAINT FK_mh_user_role_user_id FOREIGN KEY (user_id) REFERENCES mh_user (id)
) ENGINE=InnoDB;

-- Create a temporary join table from the old mh_user and mh_role table.
CREATE TEMPORARY TABLE IF NOT EXISTS mh_user_role_tmp AS (SELECT u.username, u.organization, role FROM mh_user_tmp AS u, mh_role_tmp AS r WHERE r.username = u.username AND r.organization = u.organization);

-- Fill the mh_user_role table by the temporary join table with the assigned users and roles from the new mh_user and mh_role table.
INSERT INTO mh_user_role (user_id, role_id) SELECT u.id, r.id FROM mh_user AS u, mh_role AS r, mh_user_role_tmp AS t WHERE u.username = t.username AND r.name = t.role;

-- Drop the temporary and old user and role table.
DROP TABLE mh_user_role_tmp;
DROP TABLE mh_role_tmp;
DROP TABLE mh_user_tmp;

CREATE TABLE mh_group (
  id bigint(20) NOT NULL,
  group_id varchar(128) DEFAULT NULL,
  description varchar(255) DEFAULT NULL,
  name varchar(128) DEFAULT NULL,
  role varchar(255) DEFAULT NULL,
  organization varchar(128) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_group_0 UNIQUE (group_id, organization),
  CONSTRAINT FK_mh_group_organization FOREIGN KEY (organization) REFERENCES mh_organization (id)
) ENGINE=InnoDB;

CREATE TABLE mh_group_member (
  JpaGroup_id bigint(20) NOT NULL,
  MEMBERS varchar(255) DEFAULT NULL
) ENGINE=InnoDB;

CREATE TABLE mh_group_role (
  group_id bigint(20) NOT NULL,
  role_id bigint(20) NOT NULL,
  PRIMARY KEY (group_id,role_id),
  CONSTRAINT UNQ_mh_group_role_0 UNIQUE (group_id, role_id),
  CONSTRAINT FK_mh_group_role_group_id FOREIGN KEY (group_id) REFERENCES mh_group (id),
  CONSTRAINT FK_mh_group_role_role_id FOREIGN KEY (role_id) REFERENCES mh_role (id)
) ENGINE=InnoDB;

CREATE TABLE mh_acl_managed_acl (
  pk BIGINT(20) NOT NULL,
  acl TEXT NOT NULL,
  name VARCHAR(128) NOT NULL,
  organization_id VARCHAR(128) NOT NULL,
  PRIMARY KEY (pk),
  CONSTRAINT UNQ_mh_acl_managed_acl_0 UNIQUE (name, organization_id)
) ENGINE=InnoDB;

CREATE TABLE mh_acl_episode_transition (
  pk BIGINT(20) NOT NULL,
  workflow_params VARCHAR(255) DEFAULT NULL,
  application_date DATETIME DEFAULT NULL,
  workflow_id VARCHAR(128) DEFAULT NULL,
  done TINYINT(1) DEFAULT 0,
  episode_id VARCHAR(128) DEFAULT NULL,
  organization_id VARCHAR(128) DEFAULT NULL,
  managed_acl_fk BIGINT(20) DEFAULT NULL,
  PRIMARY KEY (pk),
  CONSTRAINT UNQ_mh_acl_episode_transition_0 UNIQUE (episode_id, organization_id, application_date),
  CONSTRAINT FK_mh_acl_episode_transition_managed_acl_fk FOREIGN KEY (managed_acl_fk) REFERENCES mh_acl_managed_acl (pk)
) ENGINE=InnoDB;

CREATE TABLE mh_acl_series_transition (
  pk BIGINT(20) NOT NULL,
  workflow_params VARCHAR(255) DEFAULT NULL,
  application_date DATETIME DEFAULT NULL,
  workflow_id VARCHAR(128) DEFAULT NULL,
  override TINYINT(1) DEFAULT 0,
  done TINYINT(1) DEFAULT 0,
  organization_id VARCHAR(128) DEFAULT NULL,
  series_id VARCHAR(128) DEFAULT NULL,
  managed_acl_fk BIGINT(20) DEFAULT NULL,
  PRIMARY KEY (pk),
  CONSTRAINT UNQ_mh_acl_series_transition_0 UNIQUE (series_id, organization_id, application_date),
  CONSTRAINT FK_mh_acl_series_transition_managed_acl_fk FOREIGN KEY (managed_acl_fk) REFERENCES mh_acl_managed_acl (pk)
) ENGINE=InnoDB;

CREATE TABLE mh_user_ref (
  id bigint(20) NOT NULL,
  username varchar(128) DEFAULT NULL,
  last_login datetime DEFAULT NULL,
  email varchar(255) DEFAULT NULL,
  name varchar(255) DEFAULT NULL,
  login_mechanism varchar(255) DEFAULT NULL,
  organization varchar(128) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_user_ref_0 UNIQUE (username, organization),
  CONSTRAINT FK_mh_user_ref_organization FOREIGN KEY (organization) REFERENCES mh_organization (id)
) ENGINE=InnoDB;

CREATE TABLE mh_user_ref_role (
  user_id bigint(20) NOT NULL,
  role_id bigint(20) NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT UNQ_mh_user_ref_role_0 UNIQUE (user_id, role_id),
  CONSTRAINT FK_mh_user_ref_role_role_id FOREIGN KEY (role_id) REFERENCES mh_role (id),
  CONSTRAINT FK_mh_user_ref_role_user_id FOREIGN KEY (user_id) REFERENCES mh_user_ref (id)
) ENGINE=InnoDB;