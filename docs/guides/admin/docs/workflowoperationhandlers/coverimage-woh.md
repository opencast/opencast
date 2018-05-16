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

## Metadata

If no metadata is passed by using the configuration key `metadata`, the default metadata is passed to the cover image
service which looks like the following example:

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

## Stylesheet

The cover image service uses the Xalan XSLT 1.0 processor to transform an XML stylesheet to an SVG image.

The general structure of the stylesheet is expected to look like this:

    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

      <xsl:param name="width" />
      <xsl:param name="height" />
      <xsl:param name="posterimage" />

      <xsl:template match="/">

        <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.1">
          <xsl:attribute name="width">
            <xsl:value-of select="$width" />
          </xsl:attribute>
          <xsl:attribute name="height">
            <xsl:value-of select="$height" />
          </xsl:attribute>

          <!-- Your SVG content -->

      </xsl:template>

    </xsl:stylesheet>

The variables `width`, `height` and `posterimage` will be set to the values of the respective configuration keys.

As a starting point for your own template you best take a look at file `etc/branding/coverimage.xsl`.

### Using XLST Extensions

Xalan is a powerful XSLT 1.0 processor that comes with a rich feature set. For example, it is possible to
execute JavaScript or Java code directly within the stylesheet.

For commonly used tasks it is simpler, however, to make use of available XSLT Extensions.

#### Opencast Extensions

The package org.opencastproject.coverimage.impl.xsl provides classes supposed to be used within XSL stylesheets.

To make use of those classes, you need to reference the package from your XSL stylesheet:

    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
      xmlns:opencast="xalan://org.opencastproject.coverimage.impl.xsl" exclude-result-prefixes="opencast"
      extension-element-prefixes="opencast">
    </xsl:stylesheet>

Later on, you can use methods of those classes as shown in the following example:

    <tspan class="title" y="30%" x="50%">
      <xsl:value-of select="opencast:XsltHelper.split(metadata/title, 30, 1, false())" />
    </tspan>

Note: In XSLT, use `true()` and `false()` for boolean literals (`true` and `false` won't work since those are not
keywords in XSLT)

The following classes are provided by the org.opencastproject.coverimage.impl.xsl package:

**class XsltHelper**

*String split(String text, int maxChars, int line, boolean isLastLine)*

This method can be used to break strings over multiple lines and to abbreviate strings that are too using ellipsis.

|Parameter  |Description                                                  |
|-----------|-------------------------------------------------------------|
|text       |Input string                                                 |
|maxChars   |Maximum number of characters per line                        |
|line       |Number of line                                               |
|isLastLine |Whether `line` is the last line used to represent the `text` |

*Example*

To use at most two lines (max. 30 characters per line) to represent a string `metadata/title` and abbreviate the string
if two lines aren't enough:

    <tspan class="title" y="30%" x="50%">
      <xsl:value-of select="opencast:XsltHelper.split(metadata/title, 30, 1, false())" />
    </tspan>
    <tspan class="title" dy="10%" x="50%">
      <xsl:value-of select="opencast:XsltHelper.split(metadata/title, 30, 2, true())" />
    </tspan>

#### EXSLT Extensions

Xalan supports most of the XSLT extensions of the EXSLT community (see [[1]](http://exslt.org/)). In doubt consult
[[2]](http://xml.apache.org/xalan-j/extensionslib.html) for more information about Xalan's implementation of the
EXSLT extensions.

Please find an example of how to use EXSLT extensions below:

    <xsl:stylesheet version="1.0"
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:dcterms="http://purl.org/dc/terms/"
      xmlns:date="http://exslt.org/dates-and-times"
      xmlns:opencast="xalan://org.opencastproject.coverimage.impl.xsl" exclude-result-prefixes="date"
      extension-element-prefixes="date">

      <!-- [...] -->

      <tspan class="presentationdate" dy="12%" x="50%">
        <xsl:value-of select="date:format-date(metadata/date, 'MMMMMMMMMM dd, YYYY, HH:mm:ss')" />
      </tspan>

      <!-- [...] -->

    </xsl:stylesheet>

In this example, the function `format-date` of the EXSLT dates-and-times functions library is used to format a date.

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
        <configuration key="metadata">
          <![CDATA[<meta><title>my custom title</title><special>very special</special></meta>]]>
        </configuration>
        <configuration key="width">1920</configuration>
        <configuration key="height">1080</configuration>
        <configuration key="posterimage-flavor">presenter/player+preview</configuration>
        <configuration key="target-flavor">image/cover</configuration>
     </configurations>
    </operation>

