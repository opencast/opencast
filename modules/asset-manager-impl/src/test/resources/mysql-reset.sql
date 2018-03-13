# separate each statement with a blank line
# reset the test database

SET FOREIGN_KEY_CHECKS = 0;

# other tables

DROP TABLE IF EXISTS SEQUENCE;

DROP TABLE IF EXISTS oc_organization;

# archive tables

DROP TABLE IF EXISTS oc_archive_episode;

DROP TABLE IF EXISTS oc_archive_asset;

DROP TABLE IF EXISTS oc_archive_version_claim;

# assets tables

DROP TABLE IF EXISTS oc_assets_snapshot;

DROP TABLE IF EXISTS oc_assets_asset;

DROP TABLE IF EXISTS oc_assets_properties;

DROP TABLE IF EXISTS oc_assets_version_claim;

####
# base setup
##

# organization

CREATE TABLE oc_organization (
  id             VARCHAR(128) NOT NULL,
  anonymous_role VARCHAR(255),
  name           VARCHAR(255),
  admin_role     VARCHAR(255),
  PRIMARY KEY (id)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

INSERT INTO oc_organization (id, anonymous_role, name, admin_role)
VALUES ('mh_default_org', 'ROLE_ANONYMOUS', 'Default', 'ROLE_ADMIN');

# sequence

CREATE TABLE SEQUENCE (
  SEQ_NAME  VARCHAR(50) PRIMARY KEY NOT NULL,
  SEQ_COUNT DECIMAL(38, 0)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

INSERT INTO SEQUENCE (SEQ_NAME, SEQ_COUNT) VALUES ('SEQ_GEN', 0);

SET FOREIGN_KEY_CHECKS = 1;
