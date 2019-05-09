-- Due to MH-13514 Add descriptive node names to hosts
ALTER TABLE oc_host_registration ADD COLUMN node_name VARCHAR(255) AFTER host;

ALTER TABLE oc_ibm_watson_transcript_job CHANGE COLUMN media_package_id mediapackage_id varchar(128);
ALTER TABLE oc_aws_asset_mapping CHANGE COLUMN media_package_element mediapackage_element varchar(128);
ALTER TABLE oc_aws_asset_mapping CHANGE COLUMN media_package mediapackage varchar(128);
