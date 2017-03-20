-- MH-12013
CREATE TABLE mh_oaipmh (
  id VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  repo_id VARCHAR(255) NOT NULL,
  series_id VARCHAR(128),
  deleted tinyint(1) DEFAULT '0',
  modification_date DATETIME DEFAULT NULL,
  mediapackage_xml TEXT(65535) NOT NULL,
  series_dublincore_xml TEXT(65535),
  episode_dublincore_xml TEXT(65535),
  series_acl_xml TEXT(65535),
  PRIMARY KEY (id, repo_id, organization),
  CONSTRAINT UNQ_mh_oaipmh UNIQUE (modification_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX IX_mh_oaipmh_modification_date ON mh_oaipmh (modification_date);

-- set to current date and time on insert
CREATE TRIGGER mh_init_oaipmh_date BEFORE INSERT ON `mh_oaipmh`
FOR EACH ROW SET NEW.modification_date = NOW();

-- set to current date and time on update
CREATE TRIGGER mh_update_oaipmh_date BEFORE UPDATE ON `mh_oaipmh`
FOR EACH ROW SET NEW.modification_date = NOW();

CREATE TABLE mh_oaipmh_harvesting (
  url VARCHAR(255) NOT NULL,
  last_harvested datetime,
  PRIMARY KEY (url)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- MH-12015
update mh_role set name='ROLE_API_GROUPS_CREATE' where name='ROLE_API_GOURPS_CREATE';

