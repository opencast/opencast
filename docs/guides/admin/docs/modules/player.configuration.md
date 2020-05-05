Opencast Player - Configuration
===============================

The configurations for the player are done for each tenant. So the configuration keys are located in
`.../etc/ui-config/<tenant>/theodul/config.yml`

The default tenant for opencast is `mh_default_org`

Select the Opencast Player
------------------------------

To activate the player set in each tenant this line in the file `.../etc/org.opencastproject.organization-<tenant>.cfg`.


    prop.player=/paella/ui/watch.html?id=#{id}
