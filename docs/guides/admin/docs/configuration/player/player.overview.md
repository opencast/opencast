Opencast Player
===============================

Opencast ships with a default video player, the Paella Player.

Currently Opencast ships with two versions of the Paella Player. Version 7 is the "new player" and the recommended
default. Version 6 is still shipped for legacy reasons, but will be phased out eventually.

Configuration
------------------------------
The configurations for the player are done for each tenant. So the configuration keys are located in
`.../etc/ui-config/<tenant>/<player>/<file>`

The default tenant for opencast is `mh_default_org`

### Select the Opencast Player

To change the default player for a tenant, set the following key in `.../etc/org.opencastproject.organization-<tenant>.cfg`.

    prop.player=/paella7/ui/watch.html?id=#{id}
