PublishYoutubeWorkflowOperation
===============================


Description
-----------

The PublishYoutubeWorkflowOperation publishes a single stream to YouTube.  This stream must meet YouTube's format 
requirements, and may consist of audio and/or video.  If you want to publish both your presenter and presentation
streams we suggest using the [Composite](composite-woh.md) workflow operation handler to prepare a composite file
with both streams inside of it.  The default Opencast workflow prepares a video using this method. 


Parameter Table
---------------

|configuration keys         |description                                                                   |
|---------------------------|------------------------------------------------------------------------------|
|source-flavors             |The flavors to publish to YouTube                                             |
|source-tags                |The tags to publish to YouTube                                                |


Operation Example
-----------------

This snippet comes from `ng-partial-publish.xml` and takes the input `presenter` and `presentation` video, stitches
them side by side, and then uploads it to YouTube.

    <operation
      id="composite"
      description="Create YouTube compatible output"
      if="${publishToYouTube}"
      fail-on-error="true"
      exception-handler-workflow="ng-partial-error">
      <configurations>
        <configuration key="source-flavor-lower">presentation/themed</configuration>
        <configuration key="source-flavor-upper">presenter/themed</configuration>
        <configuration key="encoding-profile">mp4-preview.dual.http</configuration>
        <configuration key="target-flavor">composite/delivery</configuration>
        <configuration key="target-tags">youtube</configuration>
        <configuration key="output-resolution">1280x800</configuration>
        <configuration key="output-background">0x000000FF</configuration>
        <configuration key="layout">preview</configuration>
        <configuration key="layout-preview">
          {"horizontalCoverage":0.5,"anchorOffset":{"referring":{"left":1.0,"top":0.0},"reference":{"left":1.0,"top":0.0},"offset":{"x":0,"y":0}}};
          {"horizontalCoverage":0.5,"anchorOffset":{"referring":{"left":0.0,"top":0.0},"reference":{"left":0.0,"top":0.0},"offset":{"x":0,"y":0}}};
        </configuration>
      </configurations>
    </operation>

    <operation
      id="publish-youtube"
      if="${publishToYouTube}"
      max-attempts="2"
      exception-handler-workflow="ng-partial-error"
      description="Publishing to YouTube">
      <configurations>
        <configuration key="source-tags">youtube</configuration>
      </configurations>
    </operation>
