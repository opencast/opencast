Paella plugin: org.opencast.paella.cookieconsent
================================================

This plugin adds a cookie consent banner to comply with international privacy laws.

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enabled the `org.opencast.paella.cookieconsent` plugin.

```json
{
    "org.opencast.paella.cookieconsent": {
        "enabled": true,
        "side": "right"
    }    
}
```
