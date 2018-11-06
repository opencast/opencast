Overview
========

Video content is often represented visually by using thumbnail images and metadata. Think of, for example, of list
of videos displayed as a thumbnail image together with the title and description for each video.

On this page, this is the image that we refer to as thumbnail.

Since having high quality thumbnails is important, the Opencast video editor comes with built-in support for
thumbnails. We distinguish between three kinds of thumbnails:

  1. The **Thumbnail Preview** is a preview of the thumbnail shown in the video editor only
  2. The **Default Thumbnail** is automatically generated
  3. A **Snapshot Thumbnail** can be extracted from the video by the user
  4. An **Uploaded Thumbnail** is uploaded by the user

As both the video editor and your workflows need to work together to enable full support of this feature,
the Admin UI comes with a number of configuration options that allows this feature to be integrated into
your workflow configuration.

This page describes the configuration options of the Admin UI relevant for the thumbnail support of the video editor.
These options can be adjusted in the configuration file `etc/org.opencastproject.adminui.cfg`.

Thumbnail Preview
-----------------

The video editor displays a preview of the actual thumbnail at any time. This is a downscaled version of the actual
thumbnail image.

The video editor expects this thumbnail preview image to be published in the publication channel `internal` as
attachment with the flavor as specified in the Admin UI configuration:

    # Default: thumbnail/preview
    #thumbnail.preview.flavor=thumbnail/preview

When the user chooses a thumbnail in the video editor, this image will be automatically created and published.

The thumbnail preview is automatically downscaled if necessary using the following encoding profile:

    # Default: editor.thumbnail.preview.downscale
    #thumbnail.preview.profile.downscale=editor.thumbnail.preview.downscale

Note that this image is supposed to be used by the Admin UI only.

Default Thumbnail
-----------------

This thumbnail is supposed to be automatically generated without user interaction.

When the user chooses the default thumbnail in the video editor, Opencast will automatically generate and publish
an updated thumbnail preview image.

The default thumbnail image is extracted from a track identified by the following configuration:

    # Default: presenter
    #thumbnail.source.flavor.type.primary=presenter

    # Default: presentation
    #thumbnail.source.flavor.type.secondary=presentation

    # Default: source
    #thumbnail.source.flavor.subtype=source

In this example, the default thumbnail would be extracted from the track with the flavor `presenter/source` or, if
no such track is available, a track with the flavor `presentation/source`.

The relative position within the edited video where the default thumbnail is extracted can be configured:

    # Default: 1.0
    #thumbnail.default.position=1.0

Opencast will set the following processing settings for the event being edited:

    - `thumbnailType` is set to `0` to indicate that the default thumbnail is used
    - `thumbnailPosition` is set to the absolute position of the video where the default thumbnail should be extracted

Snapshot Thumbnail
------------------

In case the user is not happy with the automatically generated default thumbnail, the user can extract a thumbnail
at an arbitrary position within the video.

The snapshot thumbnail will be extracted from tracks identified by the following configuration properties:

    # Default: presenter/source
    #sourcetrack.left.flavor=presenter/source

    # Default: presentation/source
    #sourcetrack.right.flavor=presentation/source

Note that the user can choose between "Extract from video", "Extract from left video" and "Extract from right video".
In any case, the video editor ensures that the correct source track flavor is used.

Opencast will set the following processing settings for the event being edited:

    - `thumbnailType` is set to `1` to indicate that a snapshot thumbnail is used
    - `thumbnailPosition` is set to the absolute position of the video where the snapshot thumbnail should be extracted
    - `thumbnailTrack` is set to the type of the flavor of the source track which is `presenter` or `presentation`
    in this example.

Uploaded Thumbnail
------------------

The most flexible option is to upload an image to be used as thumbnail.

When the user uploads an image in the video editor, Opencast will automatically generate and publish the thumbnail
preview and creates a new media package snapshot after adding the uploaded image as attachment to the media package.

This attachment will have the following flavor:

    # Default: thumbnail/source
    #thumbnail.uploaded.flavor=thumbnail/source

Additionally, the following tags are added to the attachment:

    # Default: archive
    #thumbnail.uploaded.tags=archive

**IMPORTANT:** Please ensure that all workflows in your setup will always include this attachment when taking
snapshots using the workflow operation [snapshot](../../workflowoperationhandlers/snapshot-woh.md) by setting its
configuration key `source-tags` and/or `source-flavor` appropriately.

Opencast will set the following processing settings for the event being edited:

    - `thumbnailType` is set to `2` to indicate that an uploaded thumbnail is used

Automatic Distribution
----------------------

To avoid the situation that a user needs to start a workflow just to update the thumbnail on the publication channels,
Opencast supports automatic distribution of thumbnail images for publication channels that support incremental
publication.

Currently, this is supported for External API publication channels (created by WOH publish-configure) and OAI-PMH
publication channels (created by WOH publish-oaipmh).

The automatic distribution of thumbnail images can be enabled in the configuration:

    # Default: false
    #thumbnail.auto.distribution=false

If automatic distribution is enabled, Opencast will automatically create and publish the thumbnail.

The following encoding profile is used for thumbnail extraction (default thumbnail and snapshot thumbnail):

    # Default: search-cover.http
    #thumbnail.encoding.profile=search-cover.http

To configure the flavor and tags of the thumbnail image attachments you can set the following values:

    # Default: */search+preview
    #thumbnail.publish.flavor=*/search+preview

    # Default: engage-download
    #thumbnail.publish.tags=engage-download

Note here that the publish flavor can contain wildcards which will be applied against the configured source flavor.

The OAI-PMH channel can be configured:

    # Default: default
    #oaipmh.channel=default

