Opencast Player - Configuration
===============================

The configurations for the player are done for each tenant. So the configuration keys are located in
`.../etc/org.opencastproject.organization-mh_default_org.cfg`.

Select the Opencast Player
------------------------------

To activate the player set:

    prop.player=/engage/theodul/ui/core.html


Configuration
-------------

|Property                              | Description                          | Options                                |
|--------------------------------------|--------------------------------------|----------------------------------------|
|`prop.logo_player`                    | Logo in the top right corner         | Any URL or local path to an image file |
|`prop.player.positioncontrols`        | Position of player controls          | `top`, `bottom` (default)              |
|`prop.player.mastervideotype`         | Default flavor of the master video * | Any flavor or nothing                  |
|`prop.player.hide_video_context_menu` | Hide browser context menu for videos | `true`, `false`                        |
|`prop.show_embed_links`               | Show player embed code               | `true`, `false`                        |
|`prop.link_mediamodule`               | If to link to the media module       | `true`, `false`                        |
|`prop.player.shortcut.*`              | Keyboard shortcut specifications     | Any key                                |


### Master Video Flavor

This specifies the flavor used to select the master video (the video on the left side in the video display). This video
file also provides the audio. If not set or no video of the specified type is available, the videos are taken in their
sequence within the media package.
