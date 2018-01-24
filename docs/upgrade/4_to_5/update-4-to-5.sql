-- Remove duplicated index
DROP INDEX IF EXISTS UNQ_mh_user_ref_role ON mh_user_ref_role;

-- Add missing foreign keys
ALTER TABLE mh_user_settings
  ADD CONSTRAINT `FK_mh_user_setting_organization`
  FOREIGN KEY (`organization`)
  REFERENCES `mh_user` (`organization`);
ALTER TABLE mh_user_settings
  ADD CONSTRAINT `FK_mh_user_setting_username`
  FOREIGN KEY (`username`)
  REFERENCES `mh_user` (`username`);
CREATE INDEX IX_mh_user_setting_organization ON mh_user_settings (organization);

-- Ensure replies to comments are deleted if the comments are deleted
ALTER TABLE mh_event_comment_reply
  DROP FOREIGN KEY `FK_mh_event_comment_reply_mh_event_comment`;
ALTER TABLE mh_event_comment_reply
  ADD CONSTRAINT `FK_mh_event_comment_reply_mh_event_comment`
  FOREIGN KEY (`event_comment_id`)
  REFERENCES `mh_event_comment` (`id`)
  ON DELETE CASCADE;
