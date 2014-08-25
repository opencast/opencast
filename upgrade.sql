DROP INDEX IX_mh_user_action_user_id ON mh_user_action (user_id);
DROP INDEX IX_mh_user_action_session_id ON mh_user_action (session_id);

ALTER TABLE mh_user_action DROP COLUMN user_id;
ALTER TABLE mh_user_action DROP COLUMN user_ip;

CREATE TABLE mh_user_session (
  session_id VARCHAR(50) NOT NULL,
  user_ip VARCHAR(255),
  user_agent VARCHAR(255),
  user_id VARCHAR(255),
  PRIMARY KEY (session_id)
) ENGINE=InnoDB

ALTER TABLE mh_user_action ADD CONSTRAINT FK_mh_user_action_session_id FOREIGN KEY (session_id) REFERENCES mh_user_session (session_id);

CREATE INDEX IX_mh_user_session_user_id ON mh_user_session (user_id);
