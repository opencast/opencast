ALTER TABLE oc_ibm_watson_transcript_job CHANGE COLUMN media_package_id mediapackage_id varchar(128);
ALTER TABLE oc_aws_asset_mapping CHANGE COLUMN media_package_element mediapackage_element varchar(128);
ALTER TABLE oc_aws_asset_mapping CHANGE COLUMN media_package mediapackage varchar(128);
