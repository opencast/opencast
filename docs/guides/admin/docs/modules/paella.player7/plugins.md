Paella player plugins
=====================

Almost every paella feature is a plugin that can be enabled/disabled by each organization.
You need to modify the `plugins` section of the [paella config file](configuration.md).

To enable/disable a plugin you need to set the plugin `enable` property to `true`/`false`.

Example:
```
{
  ...
  "plugins": {
    "list": {
      "org.opencast.paella.downloadsPlugin": {
        "enabled": true
      },
      ...
    }
  }
}
```


Plugins provided by Paella player
---------------------------------

To view a full list of plugins, see the the paella [documentation page](https://paellaplayer.upv.es/docs/)


Plugins provided by Opencast
----------------------------

The paella bundle for Opencast comes with a list of plugins to integrate with Opencast

Plugin                                             | Description 
---------------------------------------------------|:------------
[org.opencast.paella.cookieconsent](plugins/org.opencast.paella.cookieconsent.md)                                     | Adds a cookie consent banner to comply with international privacy laws.
[org.opencast.paella.descriptionPlugin](plugins/org.opencast.paella.descriptionPlugin.md)                             | Adds a panel with the video description information.
[org.opencast.paella.editorPlugin](plugins/org.opencast.paella.editorPlugin.md)                                       | Adds a button to be access to the episode editor.
[org.opencast.paella.episodesFromSeries](plugins/org.opencast.paella.episodesFromSeries.md)                           | Show a list of videos in the same series.
[org.opencast.paella.loginPlugin](plugins/org.opencast.paella.loginPlugin.md)                                         | Adds a button to be able to login.
[org.opencast.paella.opencast.userTrackingDataPlugin](plugins/org.opencast.paella.opencast.userTrackingDataPlugin.md) | Allows to use Opencast usertracking service to track usage data.
[org.opencast.paella.matomo.userTrackingDataPlugin](plugins/org.opencast.paella.matomo.userTrackingDataPlugin.md)     | Allows to use Matomo service to track usage data.
[org.opencast.paella.transcriptionsPlugin](plugins/org.opencast.paella.transcriptionsPlugin.md)                       | Adds a panel to show the OCR transcriptions.
[org.opencast.paella.versionButton](plugins/org.opencast.paella.versionButton.md)                                     | Adds a button to show the player version.
