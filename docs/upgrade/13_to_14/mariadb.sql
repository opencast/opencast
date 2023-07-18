DROP INDEX IF EXISTS IX_oc_job_statistics ON oc_job;

-- Clean up bundle information from nodes which no longer exist.
-- This is done automatically for every node if they are shut down *if* they are shut down properly.
-- We can safely do this since the Opencast cluster should be completely shut down during the database migration.
truncate oc_bundleinfo;
