ALTER TABLE oc_series ADD creator_username VARCHAR(128);
ALTER TABLE oc_series ADD creator_name VARCHAR(255);
UPDATE oc_series SET creator_username = '', creator_name = '' WHERE creator_username IS NULL;
ALTER TABLE oc_series MODIFY creator_username VARCHAR(128) NOT NULL;

-- Drop table so it can be recreated.  Previous functionality was unused, and broken even if it was
drop table oc_user_settings;
