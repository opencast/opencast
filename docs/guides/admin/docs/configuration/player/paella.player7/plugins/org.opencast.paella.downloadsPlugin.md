Paella plugin: org.opencast.paella.downloadsPlugin
=======================================================

This plugin adds a panel to download the videos.

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enabled the `org.opencast.paella.downloadsPlugin` plugin.

```json
{
    "org.opencast.paella.downloadsPlugin": {
        "enabled": true,

        "downloadFlavors": false,
        "downloadTags": false,
        "downloadMimeTypes": ["audio/m4a", "video/mp4"],

        "enableOnLicenses": [
            "CC-BY",
            "CC-BY-SA",
            "CC-BY-ND",
            "CC-BY-NC",
            "CC-BY-NC-SA",
            "CC-BY-NC-ND",
            "CC0"
        ],
        "enableOnWritePermission": true
    }    
}
```

The plugin is enabled when `enabled` is true and one of the two conditions (`enableOnLicenses` or `enableOnWritePermission`) is valid.

Configuration parameters:

- **enableOnLicenses**: The plugin is enabled if the `episode license` match any of the the licenses
    
    Valid values: `false` / Array strings

    Example: `[ "CC-BY", "CC-BY-SA" ]`

- **enableOnWritePermission**: The plugin is enabled if the `user` has `write` permission on the episode.

    Valid values: `true` / `false`

- **downloadFlavors**: Filter the media available to download by the track flavor.

    Valid values: `false` / Array strings

- **downloadTags**: Filter the media available to download by the track tags.

    Valid values: `false` / Array strings

- **downloadMimeTypes**: Filter the media available to download by the track mimetype.

    Valid values: `false` / Array strings
