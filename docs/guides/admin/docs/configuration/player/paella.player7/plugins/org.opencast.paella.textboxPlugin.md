Paella plugin: org.opencast.paella.textboxPlugin
=======================================================

This plugin displays textboxes in the player, based on a JSON catalog in the event tracks.  

The expected flavor type of the catalog is "textboxes" (i.e. "textboxes/source"). 

The file is of the form:
```json
[
  {
    "start": 2,
    "end": 4,
    "text": "My text here"
  },
  {
    "start": 7,
    "end": 14,
    "text": "More of my text here"
  }
]
```

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enable the `org.opencast.paella.textboxPlugin` plugin.

```json
{
    "org.opencast.paella.textboxPlugin": {
        "enabled": true
    }    
}
```
