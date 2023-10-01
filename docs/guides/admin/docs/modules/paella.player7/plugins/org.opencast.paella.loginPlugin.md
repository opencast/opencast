Paella plugin: org.opencast.paella.loginPlugin
==============================================

This plugin adds a button to be able to login.

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enabled the `org.opencast.paella.loginPlugin` plugin.

```json
{
    "org.opencast.paella.loginPlugin": {
        "enabled": true
    }    
}
```
