Overview
========

Before a video is playing, the content is often represented visually by using an image. 
This image is referred to as a thumbnail.

Since having high quality thumbnails is important, the Opencast comes with built-in support for thumbnails.
Thumbnails can not only show up in the engage player, but other places too! (The video editor, the engage ui or
the ltitools for example).


Default Thumbnail
-----------------

The default workflows come with workflow operations that automatically provide your videos with thumbnails. The
operation may look something like this:

```yaml
  - id: image
    if: ${straightToPublishing}
    fail-on-error: true
    exception-handler-workflow: partial-error
    description: Creating Engage player preview image
    configurations:
      - source-flavor: '*/source'
      - target-flavor: '*/player+preview'
      - target-tags: engage-download
      - encoding-profile: player-preview.http
      - time: 1
```

Per convention, the flavor for a thumbnail is of the form

`{flavor.type}/player+preview`

so for example `presenter/player+preview`. The flavor type must always match to one of the tracks, e.g. `presenter`.
The flavor subtype can theoretically be changed. However, this requires touching quite a few configuration files and 
is thus usually not worth it.

The engage ui and ltitools use a different flavor

`{flavor.type}/search+preview`

// TODO: How to support multiple resolutions 

// TODO: Thumbnail priority

Editor
-----

The editor allows for generating or uploading new thumbnails. To enable thumbnail editing, set

```txt
[thumbnail]
show = true
```

in `etc/ui-config/mh_default_org/editor/editor-settings.toml`.

In order for the default thumbnails to show in the editor, they need to be published to the internal publication.
This is done by using a workflow operation akin to the following example.

```yaml
  - id: publish-configure
    exception-handler-workflow: partial-error
    description: Publish to preview publication channel
    configurations:
      - download-source-flavors: '*/preview,*/player+preview'
      - channel-id: internal
      - url-pattern: ${org_org_opencastproject_admin_ui_url!'http://localhost:8080'}/editor-ui/index.html?id=${event_id}
      - check-availability: false
```

Changing the thumbnail of a track will cause the editor to add a workflow variable to the next workflow.  
The workflow variable is of the shape `{flavor.type}/thumbnail_edited` and is used to the tell workflow
to use the thumbnail from the editor instead of the default generated one.

You can find further settings in this config file `etc/org.opencastproject.editor.EditorServiceImpl.cfg`. Normally
you will not need to touch these.
