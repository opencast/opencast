Paella plugin: org.opencast.paella.matomo.userTrackingDataPlugin
================================================================

This plugin allows to use [Matomo service](https://matomo.org/) to track usage data. 

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enabled the `org.opencast.paella.matomo.userTrackingDataPlugin` plugin.

```json
{
    "org.opencast.paella.matomo.userTrackingDataPlugin": {
        "enabled": true,
        "context": [
            "userTracking"
        ],
        "server": "//matomo.server.com/",
        "siteId": "1",
        "matomoGlobalLoaded": false,
        "cookieType": "tracking",
        "logUserId": true,
        "events": {
            "category": "PaellaPlayer",
            "action": "${event}",
            "name": "${videoId}"
        },
        "customDimensions": {
            "1": "${videoId}",
            "2": "${metadata.series} - ${metadata.seriestitle}",
            "3": "${metadata.presenters}"
        }
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
    
- **server**: URL to matomo server
- **siteId**: matomo siteId
- **matomoGlobalLoaded**: If matomo library is loaded globally by site page or the plugin should load it.

    Keep it to `false` in Opencast.

- **cookieType**: cookie type in the cookie consent plugIn.

    Keep it to `tracking` in Opencast.

- **logUserId**: If Opecast user ID should be logued.
- **events**: If exists, user event interactions should be tracked.

    Valid values: Object with the category, action and name to track the events.

- **customDimensions**: Custom dimensions to track.

    Object. The key represents the matomo custom dimension id, the value the string to log (template vars can me used)


Template Vars:

- **videoId**: Video ID
- **metadata**: Metadata object. Metadata properties:
    - **title**
    - **subject**
    - **description**
    - **language**
    - **rights**
    - **license**
    - **series**
    - **seriestitle**
    - **presenters**
    - **contributors**
    - **startDate**
    - **duration**
    - **location**
    - **UID**
    - **type**

- **event**: Event name
