ALTER TABLE mh_event_comment CHANGE COLUMN `text` `text` TEXT(65535) NOT NULL;
ALTER TABLE mh_event_comment_reply CHANGE COLUMN `text` `text` TEXT(65535) NOT NULL;
