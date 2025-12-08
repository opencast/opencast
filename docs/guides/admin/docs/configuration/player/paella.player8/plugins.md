Paella player plugins
=====================

Almost every paella feature is a plugin that can be enabled/disabled by each organization.
You need to modify the `plugins` section of the [paella config file](configuration.md).

To enable/disable a plugin you need to set the plugin `enable` property to `true`/`false`.

Example:
```json
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

To view a full list of plugins, see the the paella [documentation page](https://paellaplayer.webs.upv.es/plugins/plugins/)


Plugins provided by Opencast
----------------------------

The paella bundle for Opencast comes with a list of plugins to integrate with Opencast

Plugin                                             | Description 
---------------------------------------------------|:------------
[org.opencast.paella.downloadsPlugin](https://polimediaupv.github.io/paella-opencast/reference/paella-opencast-plugins/plugin_downloadsplugin/)                                 | Adds a downloads button in the player interface that allows users to download video tracks and attachments. The plugin provides filtering options for tracks and attachments based on flavors, tags and MIME types.
[org.opencast.paella.editorPlugin](https://polimediaupv.github.io/paella-opencast/reference/paella-opencast-plugins/plugin_editorplugin/)                                       | Adds a button in the player interface that allows access to the Opencast video editor to modify and enhance video content.
[org.opencast.paella.eventDetailsPlugin](https://polimediaupv.github.io/paella-opencast/reference/paella-opencast-plugins/plugin_eventdetailsplugin/)    | Adds a panel with the video description information.
[org.opencast.paella.loginPlugin](https://polimediaupv.github.io/paella-opencast/reference/paella-opencast-plugins/plugin_loginplugin/)                                         | Adds a login button in the player interface that allows users to authenticate and access additional features and personalized content.
[org.opencast.paella.matomo.userTrackingDataPlugin](https://polimediaupv.github.io/paella-opencast/reference/paella-opencast-plugins/plugin_matomo_usertrackingdataplugin/)     | Allows to use Matomo service to track usage data.
[org.opencast.paella.data.relatedVideosDataPlugin](https://polimediaupv.github.io/paella-opencast/reference/paella-opencast-plugins/plugin_relatedvideosdataplugin/)                           | This data plugin retrieves and provides access to videos related to the current video event. It searches for other videos in the same series and makes them available to other plugins that need related video functionality.
[org.opencast.paella.data.relatedDocumentsDataPlugin](https://polimediaupv.github.io/paella-opencast/reference/paella-opencast-plugins/plugin_relateddocumentsdataplugin/) | This data plugin extracts and provides access to documents related to the current video event. It retrieves content from Opencast tracks and attachments based on configured flavors and makes them available to other plugins that need related document functionality.
