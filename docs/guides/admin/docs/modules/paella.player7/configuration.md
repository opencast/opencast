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

Change the default colors
-------------------------

To customize the theme modify the `.../etc/ui-config/<tenant>/paella7/custom_theme.css` file.

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