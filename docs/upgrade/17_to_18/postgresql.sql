ALTER TABLE oc_series ADD creator_username VARCHAR(128);
ALTER TABLE oc_series ADD creator_name VARCHAR(255);
UPDATE oc_series SET creator_username = '', creator_name = '' WHERE creator_username IS NULL;
ALTER TABLE oc_series ALTER creator_username SET NOT NULL;

DROP TABLE oc_user_action;
DROP TABLE oc_user_session;
