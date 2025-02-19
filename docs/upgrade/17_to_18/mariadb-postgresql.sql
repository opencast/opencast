-- Add creator_name and creator_username column to oc_series table
ALTER TABLE oc_series ADD creator_username VARCHAR(128);
ALTER TABLE oc_series ADD creator_name VARCHAR(255);
UPDATE oc_series SET creator_username = '', creator_name = '';
ALTER TABLE oc_series MODIFY creator_username VARCHAR(128) NOT NULL;
