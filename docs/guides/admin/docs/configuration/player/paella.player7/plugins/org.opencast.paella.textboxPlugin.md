Paella plugin: org.opencast.paella.textboxPlugin
=======================================================

This plugin displays textboxes in the player, based on a JSON catalog in the event tracks.

The expected flavor type of the catalog is "textboxes" (i.e. "textboxes/source").

Start time is in seconds. The display duration is fixed at 10 seconds.
Text should be kept short, or will be cut off. 20 characters max are recommended.
Optionally, a link can be specified. Clicking on a textbox with a link will
redirect to the specified resource.

The catalog file is of the form:
```json
[
  {
    "start": 2,
    "text": "My text here"
  },
  {
    "start": 7,
    "text": "More of my text here",
    "link": "https://opencast.org",
  }
]
```

The configurations for this plugin are done for each tenant. So you need to modify the `plugins`
section of the [paella config file](../configuration.md).


Configuration
-------------

You need to enable the `org.opencast.paella.textboxPlugin` plugin.

```json
{
    "org.opencast.paella.textboxPlugin": {
        "enabled": true
    }
}
```
