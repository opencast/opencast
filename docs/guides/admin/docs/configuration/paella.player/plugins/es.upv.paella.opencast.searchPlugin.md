Paella plugin: es.upv.paella.opencast.searchPlugin
==================================================

This plugin enable searches using the OCR transcription. See the [Text Extraction Configuration](../../../textextraction/)
page to configure the service.

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enabled the `es.upv.paella.opencast.searchPlugin` plugin.

```json
{
    "es.upv.paella.opencast.searchPlugin": {
        "enabled": true
    }    
}
```
