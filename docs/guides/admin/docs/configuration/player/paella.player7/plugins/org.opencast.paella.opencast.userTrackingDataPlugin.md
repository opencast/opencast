Paella plugin: org.opencast.paella.opencast.userTrackingDataPlugin
=============================================================

This plugin allows to use [Opencast usertracking service](../../../../configuration/user-statistics.and.privacy.md)
to track usage data. 

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enabled the `org.opencast.paella.opencast.userTrackingDataPlugin` plugin.

```json
{
    "org.opencast.paella.opencast.userTrackingDataPlugin": {
        "enabled": true,
        "context": [
            "userTracking"
        ]
    }    
}
```

Configuration parameters:

- **context**: [User event tracker](https://github.com/polimediaupv/paella-user-tracking) context to use.

    Indicate which events should be captured.

    Default contiguration:
    ```json
    "es.upv.paella.userEventTracker": {
        "enabled": false,
        "context": "userTracking",
        "events": [
            "PLAY",
            "PAUSE",
            "STOP",
            "ENDED",
            "SEEK",
            "FULLSCREEN_CHANGED",
            "VOLUME_CHANGED",
            "TIMEUPDATE",
            "CAPTIONS_CHANGED",
            "BUTTON_PRESS",
            "SHOW_POPUP",
            "HIDE_POPUP",
            "ENTER_FULLSCREEN",
            "EXIT_FULLSCREEN",
            "VOLUME_CHANGED",
            "CAPTIONS_ENABLED",
            "CAPTIONS_DISABLED",
            "LAYOUT_CHANGED",
            "PLAYBACK_RATE_CHANGED",
            "VIDEO_QUALITY_CHANGED",
            "RESIZE_END"
        ]
    }
    ```
    

