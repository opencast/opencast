Paella plugin: org.opencast.paella.episodesFromSeries
========================================================

This plugin shows a list of videos in the same series.

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enabled the `org.opencast.paella.episodesFromSeries` plugin.

```json
{
    "org.opencast.paella.episodesFromSeries": {
        "enabled": true
    }    
}
```
