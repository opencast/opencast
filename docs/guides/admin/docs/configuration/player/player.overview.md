Opencast Player
===============================

Opencast ships with a default video player, the Paella Player 8.

Configuration
------------------------------
The configurations for the player are done for each tenant. So the configuration keys are located in
`.../etc/ui-config/<tenant>/<player>/<file>`

The default tenant for opencast is `mh_default_org`

### Select the Opencast Player

To change the default player for a tenant, set the following key in `.../etc/org.opencastproject.organization-<tenant>.cfg`.

    prop.player=/paella8/ui/watch.html?id=#{id}
