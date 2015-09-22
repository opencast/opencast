# CoverImageWorkflowOperationHandler

## Description

The CoverImageWorkflowOperationHandler generates a cover image based on an XSLT transformation which results in an SVG image that is rasterized as PNG as a last step.

## Parameter Table

|Name|Type|Example|Default Value|Description|
|----|----|-------|-------------|-----------|
stylesheet *|URL|file:///etc/matterhorn/branding/coverimage.xsl|-|File URI to the XSL stylesheet used to generate the SVG image
metadata|XML|<meta><title>Hello!</title></meta>|-|XML string which is passed to the XSL transformation. If parameter is not given, a default XML is handed to the transformation
width *|int|1920|-|Width of the resulting image
height *|int|1080|-|Height of the resulting image
posterimage-flavor|Flavor|image/poster|-|Flavor of a poster image which may be used as a part of the cover image (e.g. as a background)
posterimage|URL|http://flickr.com/posterimage.jpg|-|URL to a custom poster image instead of using one out of the media package
target-flavor *|Flavor|image/cover|-|Flavor of the resulting cover image
target-tags|String|archive,download|-|Comma separated list of tags to be applied to the resulting attachment.


## Operation Example

    <operation
      id="cover-image"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Create a cover image">
      <configurations>
        <configuration key="stylesheet">file:///etc/matterhorn/branding/coverimage/matterhorn-default.xsl</configuration>
        <configuration key="metadata"><![CDATA[<meta><title>my custom title</title><special>very special</special></meta>]]></configuration>
        <configuration key="width">1920</configuration>
        <configuration key="height">1080</configuration>
        <configuration key="posterimage-flavor">presenter/player+preview</configuration>
        <configuration key="target-flavor">image/cover</configuration>
     </configurations>
    </operation>


## Template

As a starting point for your own template you best take a look at file /coverimage/default-template.xsl in the resources of the bundle matterhorn-conductor. The metadata XML, which is passed to the cover image service in case you don't pass your own, looks like the following example:

    <?xml version="1.0"?>
    <metadata>
      <title>Puppy Love</title>
      <date>2014-03-24T11:21:00Z</date>
      <license>All rights reserved</license>
      <series>Superbowl Commercials</series>
      <contributors>Budweiser</contributors>
      <creators>Budweiser</creators>
      <subjects>Commercial</subjects>
    </metadata>
