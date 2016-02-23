# Upgrading Opencast to 2.2

## Database Migration
You will find the database migration script in /docs/upgrade/2.1_to_2.2/<vendor>.sql

## Distribution artifacts migration
With the introduction of stream security, there was the need to be able to prevent cross-tenants access on the download and streaming distribution artifacts. To reach that the download and streaming distribution service has been adjusted to generate their artifact URL including the tenant. Because of this new URL's all existing artifact URL's need a migration and the artifacts in the file systems need to be moved to a new location.

To update the distribution artifacts:

1. Install Opencast 2.2
2. Start Matterhorn
3. Log-in to the Karaf console on your node where the search service is running (usually presentation node) and install the opencast-migration feature by entering: `feature:install opencast-migration`
4. Check the logs for errors!
5. Restart Matterhorn.

## Configuration changes
