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
