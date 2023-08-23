Paella plugin: es.upv.paella.opencast.userTrackingSaverPlugIn
=============================================================

This plugin allows to use [Opencast Usertracking Service](../../../../configuration/user-statistics.and.privacy/)
to track usage data. 

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enabled the `es.upv.paella.opencast.userTrackingSaverPlugIn` plugin.

```json
{
    "es.upv.paella.opencast.userTrackingSaverPlugIn": {
        "enabled": true
    }    
}
```
