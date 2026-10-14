-- Bundle name column was too short for some bundle symbolic names (e.g. wrap: protocol
-- bundles installed from a long file path). Drop table so it gets recreated with the fix.
drop table oc_bundleinfo;
