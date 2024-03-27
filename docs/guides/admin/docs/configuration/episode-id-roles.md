Episode ID roles
=================================

If activated, users with a role like ROLE_EPISODE_<ID>_<ACTION> will have access to the episode with the given
identifier, without this having to be specifically specified in the ACL attached to the episode.

For example, ROLE_EPISODE_872dc4ec-ca8a-4e12-8dac-ce99784d6d29_READ will allow the user to get read access to
episode 872dc4ec-ca8a-4e12-8dac-ce99784d6d29.

Setup
--------------------

Enable `episode.id.role.access` in `etc/custom.properties`.

To make this work for the Admin UI and External API, the Elasticsearch Index needs to be updated with modified
ACLs. You can achieve this by calling the /index/rebuild/AssetManager/ACL endpoint AFTER enabling this feature
in the aforementioned configuration files.
The endpoint will reindex only event ACLs.

In case that you have custom actions configured, this will only work for the actions that were configured during the
reindex of the Elasticsearch Index. If you later add custom actions, you will have to reindex again.
