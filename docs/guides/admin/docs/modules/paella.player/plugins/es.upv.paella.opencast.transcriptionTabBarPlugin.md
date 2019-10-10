Paella plugin: es.upv.paella.opencast.transcriptionTabBarPlugin
===============================================================

This plugin adds a panel to show the OCR transcriptios. See the [Text Extraction Configuration](../../../textextraction/)
page to configure the service.

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enabled the `es.upv.paella.opencast.transcriptionTabBarPlugin` plugin.

```json
{
    "es.upv.paella.opencast.transcriptionTabBarPlugin": {
        "enabled": true
    }    
}
```
