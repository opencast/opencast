Opencast Player - Piwik Tracking Plugin
=======================================

This plugin allows to user Piwik (https://piwik.org/) to track usage data. To setup Piwik please follow the instructions
on the piwik website: https://piwik.org/docs/installation/#the-5-minut-piwik-installation

The plugin respects the [Do-Not-Track](https://en.wikipedia.org/wiki/Do_Not_Track) settings of a browser. You might also
need to consider the legal requirements of your country when you setup Piwik.

This plugin uses a Piwik javascript library that is loaded from the remote Piwik server!

Tested Piwik version: 3.0.2

The configurations for the piwik player plugin are done for each tenant. So the configuration keys are located in
`.../etc/org.opencastproject.organization-mh_default_org.cfg`.

To activate the plugin set:

    prop.player.piwik.server=http://localhost/piwik

Where localhost should be replaced with your Piwik server URL.

Configuration
-------------

### prop.player.piwik.server

The Piwik server from which the Piwik JS library will be loaded and where the dat awill be reported.

### prop.player.piwik.site_id=1

The Piwik site ID has to be numeric value. If not set this will be 1. It is recommended to use different site IDs for
each tenant that is configured in Opencast.

### prop.player.piwik.heartbeat=30

The heartbeat setting to track how long a user stayed on the player page. Set to 0 or comment this line to
disable the heartbeat.

### prop.player.piwik.track_events

This setting lets you track several player events. Add the events that you want to track to the list. Comment this
property to prevent event tracking.

Events that can be tracked:

- play: play has been pressed (will also be called if after seeking).
- pause: pause has been pressend (will also be called if before seeking).
- seek: user jumps to a different time. Time in seconds will be stored
- ended: video has reached the end
- playbackrate: user changes the playback speed (values 0.75 to 3.00)
- volume: Volume change by the user value 0.0 to 1.0
- quality: manual change of video quality (quality tag is stored)
- fullscreen: user presses fullscreen button
- focus: user selects one video to be enlarged (flavor of selected video is stored)
- layout_reset: user switches back to default layout
- zoom: user changes the zoom of the video

Tracked Data
------------

Additional to the event data that can be turned on for each event (see above), this Opencast specific data is tracked
if tracking is allowed:

- Page name as "<title of the event> - <lecturer name>"
- Custom Piwik variables:
  - "event" as "<title of the event> (<event id>)"
  - "series" as "<title of the series> (<series id>)"
  - "presenter"
  - "view_mode" which can be "desktop", "mobile" or "embed"

Heartbeat data does not show how long a video has been played but how long a viewer remained on the page, while the page
was in the foreground.