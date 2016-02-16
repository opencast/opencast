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

DROP TABLE mh_event_mh_comment;
DROP TABLE mh_message_template_mh_comment;
DROP TABLE mh_comment_mh_comment_reply;
DROP TABLE mh_comment_reply;
DROP TABLE mh_message_signature_mh_comment;
DROP TABLE mh_comment;
