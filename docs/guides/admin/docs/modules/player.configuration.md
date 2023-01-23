Opencast Player - Configuration
===============================

The configurations for the player are done for each tenant. So the configuration keys are located in
`.../etc/ui-config/<tenant>/<player>/<file>`

The default tenant for opencast is `mh_default_org`

Select the Opencast Player
------------------------------

To change the default player for a tenant, set the following key in `.../etc/org.opencastproject.organization-<tenant>.cfg`.

    prop.player=/paella/ui/watch.html?id=#{id}
