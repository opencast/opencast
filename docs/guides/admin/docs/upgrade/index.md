# Upgrading Matterhorn to 2.0

## Database Migration
You will find the database migration script in /docs/upgrade/1.6_to_2.0/<vendor>.sql

## Rebuilding Search Indices
To update the search indices:

1. Shutdown Matterhorn
2. Upgrade the database, if not already done
3. Delete (or move) your search indices

	* ${org.opencastproject.storage.dir}/searchindex
	* ${org.opencastproject.storage.dir}/seriesindex
	* ${org.opencastproject.storage.dir}/schedulerindex

4. Restart Matterhorn. Rebuilding the indices can take quite a while depending on the number of recordings in your system.

## Updating Existing Workflow Definitions
Existing workflows will not work with the new admin UI in Matterhorn 2.0. They still work with the old admin ui that is still available. 

The new admin UI needs an "admin-ng" tag in the workflow.

The new admin UI does not support hold-states anymore.

## Configuration changes
