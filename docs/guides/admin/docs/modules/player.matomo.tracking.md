Opencast Player - Matomo Tracking Plugin
=======================================

This plugin allows using [Matomo](https://matomo.org) to track usage data.
To setup Matomo please follow the instructions on the Matomo website:

- [The 5-minute Matomo Installation](https://matomo.org/docs/installation/#the-5-minute-matomo-installation)

The plugin respects the [Do-Not-Track](https://en.wikipedia.org/wiki/Do_Not_Track) settings of a browser.
Please consider the legal requirements of your country when you set up Matomo.

This plugin uses a Matomo JavaScript library that is loaded from the remote Matomo server!

Tested Matomo version: 3.0.2+ ; Matomo Analytics Cloud

The configurations for the Matomo player plugin are done for each tenant.
The configuration keys are located in `etc/ui-config/<organization>/theodul/config.yml`.

To activate the plugin set the Matomo server URL:

    server: https://matomo.example.com/matomo


Configuration
-------------

### server:

The Matomo server from which the Piwik JS library will be loaded and where the data will be reported.

### site_id: 1

The Matomo site ID has to be numeric value. If not set this will be 1. It is recommended to use different site IDs for
each tenant that is configured in Opencast.

### heartbeat: 30

The heartbeat setting to track how long a user stayed on the player page. Set to 0 or comment this line to
disable the heartbeat.

### notification: true

The plugin shows a notification about the tracking to the user. This can be disabled with this option. (Default: `true`)
Before you disable the notification, make sure that you do not violate any local regulations.

### track_events: ["play", "pause", "seek", "ended"]

This setting lets you track several player events. Add the events that you want to track to the list.
Comment this property to prevent event tracking.

Events that can be tracked:

* play: play has been pressed (will also be called if after seeking).
* pause: pause has been pressend (will also be called if before seeking).
* seek: user jumps to a different time. Time in seconds will be stored
* ended: video has reached the end
* playbackrate: user changes the playback speed (values 0.75 to 3.00)
* volume: Volume change by the user value 0.0 to 1.0
* quality: manual change of video quality (quality tag is stored)
* fullscreen: user presses fullscreen button
* focus: user selects one video to be enlarged (flavor of selected video is stored)
* layout\_reset: user switches back to default layout
* zoom: user changes the zoom of the video

Tracked Data
------------

Additional to the event data that can be turned on for each event (see above), this Opencast specific data is tracked
if tracking is allowed:

* Page name as `<title of the event> - <lecturer name>`
* Custom Matomo variables:
    * "event" as `<title of the event> (<event id>)`
    * "series" as `<title of the series> (<series id>)`
    * "presenter"
    * "view\_mode" which can be `desktop`, `mobile` or `embed`

Heartbeat data does not show how long a video has been played but how long a viewer remained on the page, while the page
was in the foreground.
