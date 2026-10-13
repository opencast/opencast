Publish YouTube Workflow Operation
==================================

ID: `publish-youtube`


Description
-----------

The publish YouTube operation publishes a single stream to YouTube. This stream must meet YouTube's format
requirements, and may consist of one audio and/or video stream. If you want to publish both your presenter and presentation
streams we suggest using the [Composite](composite-woh.md) workflow operation handler to prepare a composite file
with both streams inside of it.

### Thumbnails

YouTube has specific restrictions for custom thumbnails:
- The image must be in a supported format (e.g., JPG, PNG).
- The file size must be less than 2MB.
- The YouTube account must be verified with a phone number to upload custom thumbnails.
- For more details, see the [YouTube API documentation](https://developers.google.com/youtube/v3/docs/thumbnails/set).


Parameter Table
---------------

|configuration keys         |description                                                                   |
|---------------------------|------------------------------------------------------------------------------|
|source-flavors             |The flavors to publish to YouTube                                             |
|source-tags                |The tags to publish to YouTube                                                |
|thumbnail-flavors          |The flavors of the thumbnail to publish to YouTube                            |
|thumbnail-tags             |The tags of the thumbnail to publish to YouTube                               |


Operation Example
-----------------

```yaml
  - id: publish-youtube
    description: Publishing to YouTube
    configurations:
      - source-tags: youtube
      - thumbnail-tags: youtube-thumbnail
```
