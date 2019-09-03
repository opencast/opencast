Paella plugin: es.upv.paella.opencast.loader
============================================

This plugin configures how events are loaded into paella player.

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).

An event can have many tracks, but an institution can configure which of these tracks are played and which are not.

To do it, you need to configure the `streams` property. The `streams` property is an array of rules. The first that
matches is the one that will be applied.

Each element in the array have two properties:

- **filter**: select which devices the rule applies to.
    
    Valid devices: Android, Linux, MacOS, Windows, iOS, iPad, iPhone, iPodTouch

- **tracks**: select which tracks to import into paella.

    tracks can be selected by flavors or tags

Example:

```json
{
    "streams": [
        {
            "filter": {
                "system": ["*"]
            },
            "tracks": {
                "flavors": ["*/*"],
                "tags": []
            }
        }
    ]
}
```


Examples
--------

An institution whant to play only `*/delivery` media tracks

```json
{
    "es.upv.paella.opencast.loader": {
        "streams": [
            {
                "filter": {
                    "system": ["*"]
                },
                "tracks": {
                    "flavors": ["*/delivery"],
                    "tags": []
                }
            }
        ]
    }    
}
```

An institution whants to play `sidebyside/delivery` track on `Android` and `iOS` devices 
and `presenter/delivery` and `presentation/delivery` on the other devices

```json
{
    "es.upv.paella.opencast.loader": {
        "streams": [
            {
                "filter": {
                    "system": ["Android", "iOS"]
                },
                "tracks": {
                    "flavors": ["sidebyside/delivery"],
                    "tags": []
                }
            },
            {
                "filter": {
                    "system": ["*"]
                },
                "tracks": {
                    "flavors": ["presenter/delivery", "presentation/delivery"],
                    "tags": []
                }
            }
        ]
    }    
}
```
