Paella Player 8
===============

The Paella `(pronounced 'paeja')` [Player](https://paellaplayer.webs.upv.es) is an Open Source
JavaScript video player capable of playing an unlimited number of audio & video streams
synchronously, Live Streaming, Zoom, Captions, contributed user plugins and a lot more.
It is easy to install and customize for your own needs.


Paella 8 is the new version of the paella player and is available on Opencast 19 as an alternative player.

Have a look at the paella 8 [repository](https://github.com/polimediaupv/paella-player)
or [documentation page](https://paellaplayer.webs.upv.es/documentation).


Configuration
-------------

The configurations for the player are done for each tenant. So the configuration keys are located in
`.../etc/ui-config/<tenant>/paella8/config.json`

The default tenant for opencast is `mh_default_org`


Select the Opencast Player
------------------------------

To activate the player set for each tenant the property `prop.player` in the file `.../etc/org.opencastproject.organization-<tenant>.cfg`.


    prop.player=/paella8/ui/watch.html?id=#{id}


Cookie consent
--------------
Paella uses cookies to store some user parameters (layout configuration, volume, etc...).
And, if enabled, the paella user tracking plugin can use cookie to track the user.


To comply with GDPR, ePrivacy Directive, or any other privacy laws, you can enable the 
`es.upv.paella.cookieconsent` plugin available in `paella-extra-plugins`.
This plugin uses the [CookieConsent v3](https://cookieconsent.orestbida.com/) library to show the banner.

To Learn more about paella cookie consent you can read paella
[cookie consent documentation](https://paellaplayer.webs.upv.es/reference/cookie_consent/).

You can enable/disable the cookie consent banner in `config.json` file:

```json
{
    ...
    "es.upv.paella.cookieconsent": {
        "enabled": true,
        ...
    },
    ...
}
```
