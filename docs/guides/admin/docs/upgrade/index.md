# Upgrading Matterhorn to 2.1

## Database Migration
You will find the database migration script in /docs/upgrade/2.0_to_2.1/<vendor>.sql

## Distribution artifacts migration
With the introduction of stream security, there was the need to be able to prevent cross-tenants access on the download and streaming distribution artifacts. To reach that the download and streaming distribution service has been adjusted to generate their artifact URL including the tenant. Because of this new URL's all existing artifact URL's need a migration and the artifacts in the file systems need to be moved to a new location.

To update the distribution artifacts:

1. Shutdown Matterhorn
2. Build the matterhorn-migration bundle and put it to your presentation node where the search service is running.
3. Now start Matterhorn and check the logs for errors!
4. Stop Matterhorn. Migration has been successful, remove the matterhorn-migration bundle.

## Configuration changes
