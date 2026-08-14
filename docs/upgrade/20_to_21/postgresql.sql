-- Bundle name column was too short for some bundle symbolic names (e.g. wrap: protocol
-- bundles installed from a long file path). Drop table so it gets recreated with the fix.
drop table oc_bundleinfo;

-- add catalog storage to snapshots
ALTER TABLE `oc_assets_snapshot` ADD `episode_xml` VARCHAR(65535) DEFAULT NULL;

