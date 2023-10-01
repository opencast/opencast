Paella plugin: org.opencast.paella.editorPlugin
==============================================

This plugin adds a button to be able to login.

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enabled the `org.opencast.paella.editorPlugin` plugin.

```json
{
    "org.opencast.paella.editorPlugin": {
        "enabled": true,
        "showIfAnonymous": false,
        "showIfCanWrite": true,
        "editorUrl": "/editor-ui/index.html?id={id}"
    }    
}
```

Configuration parameters:

- **showIfAnonymous**: The button should be shown when the user is not logged in.
    
    Valid values: `true` / `false`

- **showIfCanWrite**: The button should be shown when user has write permission.

    Valid values: `true` / `false`

- **editorUrl**: URL string to access the editor.
