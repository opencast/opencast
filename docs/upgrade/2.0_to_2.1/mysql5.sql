use matterhorn;

ALTER TABLE mh_user ADD COLUMN manageable TINYINT(1) NOT NULL DEFAULT '1';