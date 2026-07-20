-- add catalog storage to snapshots
ALTER TABLE `oc_assets_snapshot` ADD `episode_xml` longtext DEFAULT NULL;