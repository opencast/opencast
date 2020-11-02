Paella player plugins
=====================

Almost every paella feature is a plugin that can be enabled/disabled by each organization.
You need to modify the `plugins` section of the [paella config file](configuration.md).

To enable/disable a plugin you need to set the plugin `enable` property to `false`.

Example:
```
{
  ...
  "plugins": {
    "list": {
      "es.upv.paella.opencast.downloadsPlugin": {
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
[es.upv.paella.opencast.descriptionPlugin](plugins/es.upv.paella.opencast.descriptionPlugin.md)                 | Adds a panel with the video description information.
[es.upv.paella.opencast.downloadsPlugin](plugins/es.upv.paella.opencast.downloadsPlugin.md)                     | Adds a panel to download the videos. 
[es.upv.paella.opencast.episodesFromSeries](plugins/es.upv.paella.opencast.episodesFromSeries.md)               | Show a list of videos in the same series.
[es.upv.paella.opencast.loader](plugins/es.upv.paella.opencast.loader.md)                                       | Configures how events are loaded into paella player.
[es.upv.paella.opencast.logIn](plugins/es.upv.paella.opencast.logIn.md)                                         | Adds a button to be able to login.
[es.upv.paella.opencast.searchPlugin](plugins/es.upv.paella.opencast.searchPlugin.md)                           | Enable searches using the OCR transcription.
[es.upv.paella.opencast.transcriptionTabBarPlugin](plugins/es.upv.paella.opencast.transcriptionTabBarPlugin.md) | Adds a panel to show the OCR transcriptions.
[es.upv.paella.opencast.userTrackingSaverPlugIn](plugins/es.upv.paella.opencast.userTrackingSaverPlugIn.md)     | Allows to use Opencast Usertracking Service to track usage data.
