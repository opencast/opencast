Paella plugin: org.opencast.paella.matomo.userTrackingDataPlugin
================================================================

This plugin allows to use [Matomo service](https://matomo.org/) to track usage data.

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enabled both the `es.upv.paella.userEventTracker` and the `org.opencast.paella.matomo.userTrackingDataPlugin` plugin.

```json
{
    "es.upv.paella.userEventTracker": {
        "enabled": true,
        "context": "userTracking",
        "events": [
            "PLAY",
            "PAUSE",
            "STOP",
            "ENDED",
            "SEEK",
            "FULLSCREEN_CHANGED",
            "VOLUME_CHANGED",
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
    },
    "org.opencast.paella.matomo.userTrackingDataPlugin": {
        "enabled": true,
        "context": [
            "userTracking"
        ],
        "server": "//matomo.server.com/",
        "siteId": "1",
        "trackerUrl": {
            "php": "matomo.php",
            "js": "matomo.js"
        },
        "matomoGlobalLoaded": false,
        "mediaAnalyticsTitle": "${videoId}",
        "cookieType": "tracking",
        "logUserId": true,
        "events": {
            "category": "PaellaPlayer",
            "action": "${event}",
            "name": "${eventData}"
        },
        "customDimensions": {
            "1": "${videoId}",
            "2": "${metadata.series} - ${metadata.seriestitle}",
            "3": "${metadata.presenters}"
        }
    }
}
```

**Configuration parameters:**

- **context**: [User event tracker](https://github.com/polimediaupv/paella-user-tracking) context to use.

    Corresponds to the `context` parameter of `es.upv.paella.userEventTracker`.
    Indicates which events should be tracked.

    Example contiguration:

    ```
    "es.upv.paella.userEventTracker": {
        "enabled": true,
        "context": "userTracking",
        "events": [
            "PLAY",
            "PAUSE",
            "STOP",
            "ENDED",
            "SEEK",
            "FULLSCREEN_CHANGED",
            "VOLUME_CHANGED",
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

- **server**: URL to Matomo server

- **siteId**: Matomo siteId

- **trackerUrl**: Matomo JavaScript and PHP files loaded by the Paella player

    Allows to define files other than the default `matomo.php` and `matomo.js` that are loaded by the player to track usage data.

- **matomoGlobalLoaded**: If Matomo library is loaded globally by site page or the plugin should load it.

    Keep it to `false` in Opencast.

- **mediaAnalyticsTitle**: Template variable that will be used as title information in the Matomo Media Analytics plugin.

    Only relevant if you are using the Matomo Media Analytics plugin. It is recommended to use the `videoId` template variable to uniquely identify events in the Matomo Media Analytics plugin. If not set, the event title will be used.

- **cookieType**: cookie type in the cookie consent plugIn.

    Keep it to `tracking` in Opencast.

- **logUserId**: If Opecast user ID should be logged.

- **events**: If exists, user event interactions should be tracked.

    Valid values: Object with the category, action and name to track the events.

- **customDimensions**: Custom dimensions to track.

    Object. The key represents the Matomo custom dimension id, the value the string to log (template vars can me used)


**Available template Vars:**

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

- **event**: Player event name (e.g. `paella:playbackRateChanged`)

- **eventData**: A text representation of the data received by the player event (e.g. for the event `paella:playbackRateChanged` eventData = `2`)
