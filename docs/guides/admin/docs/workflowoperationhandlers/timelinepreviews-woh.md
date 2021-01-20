TimelinePreviewsWorkflowOperationHandler
========================================

Description
-----------

The timeline previews operation creates preview images for the given track that can be shown when hovering above the
timeline. It will generate the in `image-count` specified number of preview images, that will all be saved in one large
image file. You can use the `source-flavor` to specify for which video the preview images will be generated. In the
engage player only the preview images of one video are shown (the first that is found), so to make sure the correct
preview images are shown, better generate them only for one video. If `source-flavor` and `source-tags` are both used, and the resultant source is source-flavor AND source-tags

Parameter Table
---------------

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|source-tags|high-res|Specifies which tagged media should be processed. This can be combined with flavor, so that the source is high-res AND \*/trimmed .|EMPTY|
|source-flavors|\*/trimmed|Specifies which media should be processed. The \*-operator can be used if the preview images should be created for all flavors with a certain subtype (like "trimmed" in the example) Hereby you can for example choose whether you want to create the timeline preview images from a presenter or a presentation video.|EMPTY|
|target-flavor|\*/timeline+preview|Specifies the flavor the new files will get. This should use the \*-operator if it was used in the source-flavor too. This flavor has to contain the words "timeline" and "preview" for the file to be found by the player.|EMPTY|
|target-tags|engage-download|Specifies the tags the new files will get.|EMPTY|
|image-count|100|Specifies the number of generated timeline preview images. In the example 100 timeline preview images will be generated and stored in a 10x10 grid in the output image|100|
|process-first-match-only|true|Only use the first resource that matches the source-flavor and/or target-tag|false|

Operation Example
-----------------

```xml
<operation
  id="timelinepreviews"
  description="Creating presentation timeline preview images">
  <configurations>
    <configuration key="source-flavor">*/trimmed</configuration>
    <configuration key="target-flavor">*/timeline+preview</configuration>
    <configuration key="target-tags">engage-download</configuration>
    <configuration key="image-count">100</configuration>
    <!-- If there is more than one file that match the source-tags, use only the first one -->
    <configuration key="process-first-match-only">true</configuration>
  </configurations>
</operation>
```
