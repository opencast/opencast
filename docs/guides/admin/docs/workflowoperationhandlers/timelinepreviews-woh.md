# TimelinePreviewsWorkflowOperationHandler

## Description
The timeline previews operation creates preview images for the given track that can be shown when hovering above the timeline. It will generate (image-size $$\times$$ image-size) many preview images, that will all be saved in one large PNG image.

## Parameter Table

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|source-flavors|*/preview|Specifies which media should be processed. The *-operator can be used if the preview images should be created for all flavors with a certain subtype (like "preview" in the example).|EMPTY|
|target-flavor|*/timeline+preview|Specifies the flavor the new files will get. This should use the *-operator if it was used in the source-flavor too.|EMPTY|
|target-tags|engage-download|Specifies the tags the new files will get.|EMPTY|
|image-size|100|Specifies the number of generated timeline preview images. In the example 100 timeline preview images will be generated and stored in a 10x10 grid in the output image|100|

## Operation Example

    <operation
      id="timelinepreviews"
      if="${publish}"
      fail-on-error="false"
      exception-handler-workflow="error"
      description="Creating presentation timeline preview images">
      <configurations>
        <configuration key="source-flavor">*/preview</configuration>
        <configuration key="target-flavor">*/timeline+preview</configuration>
        <configuration key="target-tags">engage-download</configuration>
        <configuration key="image-size">100</configuration>
      </configurations>
    </operation>
