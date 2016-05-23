ALTER TABLE mh_job ADD job_load FLOAT NOT NULL DEFAULT '1.0';
ALTER TABLE mh_job ADD blocking_job BIGINT DEFAULT NULL;

ALTER TABLE mh_host_registration ADD max_load FLOAT NOT NULL DEFAULT '1.0';
UPDATE mh_host_registration SET max_load=max_jobs;
ALTER TABLE mh_host_registration DROP max_jobs;

CREATE TABLE mh_blocking_job (
  id BIGINT NOT NULL,
  blocking_job_list BIGINT,
  job_index INTEGER,
  CONSTRAINT FK_blocking_job_id FOREIGN KEY (id) REFERENCES mh_job (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE mh_user ADD COLUMN manageable TINYINT(1) NOT NULL DEFAULT '1';

CREATE TABLE mh_event_comment (
  id BIGINT(20) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  event VARCHAR(128) NOT NULL,
  creation_date DATETIME NOT NULL,
  author VARCHAR(255) NOT NULL,
  text VARCHAR(255) NOT NULL,
  reason VARCHAR(255) DEFAULT NULL,
  modification_date DATETIME NOT NULL,
  resolved_status TINYINT(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO mh_event_comment (id, organization, event, creation_date, author, text, reason, modification_date, resolved_status)
  SELECT c.id, e.organization, e.event, c.creation_date, c.author, c.text, c.reason, c.modification_date, c.resolved_status
  FROM mh_comment as c, mh_event_mh_comment e
  WHERE c.id = e.comment;

CREATE TABLE mh_event_comment_reply (
  id BIGINT(20) NOT NULL,
  event_comment_id BIGINT(20) NOT NULL,
  creation_date DATETIME NOT NULL,
  author VARCHAR(255) NOT NULL,
  text VARCHAR(255) NOT NULL,
  modification_date DATETIME NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FK_mh_event_comment_reply_mh_event_comment FOREIGN KEY (event_comment_id) REFERENCES mh_event_comment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO mh_event_comment_reply (id, event_comment_id, creation_date, author, text, modification_date)
  SELECT r.id, c.Comment_id, r.creation_date, r.author, r.text, r.modification_date 
  FROM mh_comment_reply AS r, mh_comment_mh_comment_reply AS c;

DROP TABLE IF EXISTS mh_event_mh_comment;
DROP TABLE IF EXISTS mh_message_template_mh_comment;
DROP TABLE IF EXISTS mh_comment_mh_comment_reply;
DROP TABLE IF EXISTS mh_comment_reply;
DROP TABLE IF EXISTS mh_message_signature_mh_comment;
DROP TABLE IF EXISTS mh_comment;
DROP TABLE IF EXISTS mh_upload;