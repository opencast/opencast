-- add catalog storage to snapshots
ALTER TABLE `oc_assets_snapshot` ADD `episode_xml` VARCHAR(65535) DEFAULT NULL;