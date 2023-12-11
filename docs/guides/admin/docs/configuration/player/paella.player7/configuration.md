Paella Player 7
===============

Paella 7 is Opencast's default player.

The Paella `(pronounced 'paeja')` [Player](https://paellaplayer.upv.es) is an Open Source
JavaScript video player capable of playing an unlimited number of audio & video streams 
synchronously, Live Streaming, Zoom, Captions, contributed user plugins and a lot more. 
It is easy to install and customize for your own needs.

Paella 7 will be a complete rewrite of Paella, aiming several issues

- Allow easier integration in other platforms by leaving out the singleton design pattern.
- Easier styling and accessibility support.
- Reduce the number of dependencies.
- Allow easier long-term maintenance of the project.
- Solve the technical debt of 10 years of development.

Have a look at the paella 7 [repository](https://github.com/polimediaupv/paella-core)
or [documentation page](https://github.com/polimediaupv/paella-core/blob/main/doc/index.md).


Configuration
-------------

The configurations for the player are done for each tenant. So the configuration keys are located in
`.../etc/ui-config/<tenant>/paella7/config.json`

The default tenant for opencast is `mh_default_org`

Customize the player
--------------------

Paella player can be customized using skin packages.

Skin packages contain at least one skin configuration file, and additionally other files that are specified in the skin definition. The skin definition file is a `json` file with the following structure:

```json
{
    "styleSheets": [
        "style.css"
    ],
    "configOverrides": {
        "progressIndicator": {
            "inlineMode": true
        },
        "videoContainer": {
            "overPlaybackBar": true
        }
    },
    "icons": [
        {
            "plugin": "es.upv.paella.playPauseButton",
            "identifier": "play",
            "icon": "play-icon.svg"
        }
    ]
}
```

The skin definition file is divided into three sections:

- `styleSheets`: an array containing the list of style sheet files to be included when the skin is loaded. The file paths included here are relative to the skin definition file.
- `configOverrides`: is a json with the same properties as the main configuration file. It should be noted that in this section it is possible to include any configuration option, and not only those related to the user interface. For example: it is possible to define plugins configuration. The elements defined in this section overwrite any attribute that also exists in the main configuration file. It is important to note that configuration attributes of type array overwrite the entire array defined in the configuration, i.e. they are not added to the main array, but replaced.
- `icons`: is an array with the list of custom icons, in the form of objects with attributes `plugin`, `identifier` and `icon`. The file paths included here are relative to the skin definition file.

For more information, see the paella 7 [skin documentation](https://paellaplayer.upv.es/#/doc/skin_api.md).

You can create a new folder for you theme in `.../etc/ui-config/<tenant>/paella7/` with a file called `theme.json` and configure the theme you want to use in `config.json` file:

```json
{
    ...
    "opencast": {
        "theme": "custom_theme"
        ...
    }
    ...
}
```

And create your custom theme: `.../etc/ui-config/<tenant>/paella7/custom_theme/theme.json`.
As an example there is a basic example here: `.../etc/ui-config/<tenant>/paella7/custom_theme_example`

If no custom theme is found or there is an error loading the resources, the default opencast theme will be used.

Select the Opencast Player
------------------------------

To activate the player set for each tenant the property `prop.player` in the file `.../etc/org.opencastproject.organization-<tenant>.cfg`.


    prop.player=/paella7/ui/watch.html?id=#{id}


Cookie consent
--------------
Paella uses cookies to store some user parameters (layout configuration, volume, etc...).
And, if enabled, the paella user tracking plugin can use cookie to track the user.

To comply with GDPR, ePrivacy Directive, or any other privacy laws, the opencast player uses 
the [Terms Feed Privacy Consent](https://www.termsfeed.com/privacy-consent/) banner and the 
paella `config.json` file is configured to use it. 

To Learn more about paella cookie consent you can read paella 
[cookie consent documentation](https://paellaplayer.upv.es/#/doc/cookie_consent.md).

You can enable/disable the cookie consent banner in `config.json` file:

```json
{
    ...
    "opencast": {
        "cookieConsent": {
            "enable": true
        },
        ...
    }
    ...
}
```

Hide/Show the timeline bar on Live events
-----------------------------------------
Paella can hide the timeline bar in live events using the `opencast.hideTimeLineOnLive` property
in the configuration file.

If your live stream offers a time buffer to go back in time, you can configure paella so that
the time buffer bar is displayed and visitors will be able to go back in time.

Example:

```json
{
    ...
    "opencast": {
        "hideTimeLineOnLive": false
        ...
    }
    ...
}
```