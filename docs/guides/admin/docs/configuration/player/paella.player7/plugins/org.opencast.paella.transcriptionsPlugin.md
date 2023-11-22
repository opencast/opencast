Paella plugin: org.opencast.paella.transcriptionsPlugin
=======================================================

This plugin adds a panel to show the OCR transcriptions. See the [Text Extraction Configuration](../../../textextraction.md)
page to configure the service.

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enabled the `org.opencast.paella.transcriptionsPlugin` plugin.

```json
{
    "org.opencast.paella.transcriptionsPlugin": {
        "enabled": true
    }    
}
```
