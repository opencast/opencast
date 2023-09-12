Paella plugin: org.opencast.paella.descriptionPlugin
=======================================================

This plugin adds a panel with the video description information.

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enabled the `org.opencast.paella.descriptionPlugin` plugin.

```json
{
    "es.upv.paella.opencast.descriptionPlugin": {
        "enabled": true
    }    
}
```
