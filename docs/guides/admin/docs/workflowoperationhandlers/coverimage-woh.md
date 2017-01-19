# CoverImageWorkflowOperationHandler

## Description

The CoverImageWorkflowOperationHandler generates a cover image based on an XSLT transformation which results in an SVG
image that is rasterized as PNG as a last step.

## Parameter Table

|Name|Type|Example|Default Value|Description|
|----|----|-------|-------------|-----------|
stylesheet *|URL|file:///etc/opencast/branding/coverimage.xsl|-|File URI to the XSL stylesheet used to generate the SVG image
metadata|XML|<meta><title>Hello!</title></meta>|-|XML string which is passed to the XSL transformation. If parameter is not given, a default XML is handed to the transformation
width *|int|1920|-|Width of the resulting image
height *|int|1080|-|Height of the resulting image
posterimage-flavor|Flavor|image/poster|-|Flavor of a poster image which may be used as a part of the cover image (e.g. as a background)
posterimage|URL|http://flickr.com/posterimage.jpg|-|URL to a custom poster image instead of using one out of the media package
target-flavor *|Flavor|image/cover|-|Flavor of the resulting cover image
target-tags|String|archive,download|-|Comma separated list of tags to be applied to the resulting attachment.


## Operation Example

Operation example with metadata derived from events metadata:

    <operation
      id="cover-image"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Create a cover image">
      <configurations>
        <configuration key="stylesheet">file://${karaf.etc}/branding/coverimage.xsl</configuration>
        <configuration key="width">1920</configuration>
        <configuration key="height">1080</configuration>
        <configuration key="posterimage-flavor">presenter/coverbackground</configuration>
        <configuration key="target-flavor">presenter/player+preview</configuration>
        <configuration key="target-tags">archive, engage-download</configuration>
     </configurations>
    </operation>


Operation example with metadata provided in the operations configuration:

    <operation
      id="cover-image"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Create a cover image">
      <configurations>
        <configuration key="stylesheet">file://${karaf.etc}/branding/coverimage.xsl</configuration>
        <configuration key="metadata"><![CDATA[<meta><title>my custom title</title><special>very special</special></meta>]]></configuration>
        <configuration key="width">1920</configuration>
        <configuration key="height">1080</configuration>
        <configuration key="posterimage-flavor">presenter/player+preview</configuration>
        <configuration key="target-flavor">image/cover</configuration>
     </configurations>
    </operation>


## Template

As a starting point for your own template you best take a look at file etc/branding/coverimage.xsl. 

The metadata XML, which is passed to the cover image service, looks like the following example:

    <?xml version="1.0"?>
    <metadata>
      <title>Puppy Love</title>
      <date>2014-04-24T11:21:00</date>
      <license>All rights reserved</license>
      <description>Here is a description of the video</description>
      <series>Superbowl Commercials</series>
      <contributors>Budweiser</contributors>
      <creators>Budweiser</creators>
      <subjects>Commercial</subjects>
    </metadata>

Note that the date is localized based on your servers Java Runtime language settings.

## Libraries that can be used in XSL stylesheets

Opencast provides the XsltHelper class for easy access to some commonly used helper methods.

To make use of the XsltHelper class, you need to reference it from your XSL stylesheet:

    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <xsl:stylesheet version="1.0"
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:dcterms="http://purl.org/dc/terms/"
      xmlns:opencast="xalan://org.opencastproject.coverimage.impl.xsl" exclude-result-prefixes="opencast"
      extension-element-prefixes="opencast">

Later on, you can use methods of the XsltHelper class as shown in the following example:

    <tspan class="title" y="30%" x="50%">
      <xsl:value-of select="opencast:XsltHelper.split(metadata/title, 30, 1, false())" />
    </tspan>

Note: In XSLT, use `true()` and `false()` for boolean literals (`true` and `false` won't work since those are not 
keywords in XSLT)

The following methods are provided by the XsltHelper class:

### String split(String text, int maxChars, int line, boolean isLastLine)

This method can be used to break string over multiple lines and to abbreviate strings that are too using ellipsis.

|Parameter  |Description                                                  |
|-----------|-------------------------------------------------------------|
|text       |Input string                                                 |
|maxChars   |Maximum number of characters per line                        |
|line       |Number of line                                               |
|isLastLine |Whether `line` is the last line used to represent the `text` |

**Example**

To use at most two lines (max. 30 characters per line) to represent a string `metadata/title` and abbreviate the string if two lines aren't enough:

    <tspan class="title" y="30%" x="50%">
      <xsl:value-of select="opencast:XsltHelper.split(metadata/title, 30, 1, false())" />
    </tspan>
    <tspan class="title" dy="10%" x="50%">
      <xsl:value-of select="opencast:XsltHelper.split(metadata/title, 30, 2, true())" />
    </tspan>
