<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:date="http://exslt.org/dates-and-times"
    xmlns:opencast="xalan://org.opencastproject.coverimage.impl.xsl" exclude-result-prefixes="date opencast"
    extension-element-prefixes="date opencast">

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

      <style type="text/css" >
        <![CDATA[
          svg {
            font-size: 50pt;
            font-family: Arial;
          }
          .metadata tspan {
             text-anchor: middle;
          }
          .title {
            font-weight: bolder;
            font-size: 1.6em;
          }
          .creators {
            font-style: italic;
          }
          .creators, .presentationdate {
            font-size: 1.2em;
          }
          .description {
            font-size: 0.5em;
          }
          .license {
            font-size: 0.8em;
          }
        ]]>
      </style>

      <defs>
        <!-- Linear gradient for background -->
        <linearGradient id="lgGray" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stop-opacity="1" stop-color="#dddddd" />
          <stop offset="100%" stop-opacity="1" stop-color="#222222" />
        </linearGradient>
      </defs>

      <!-- Layer 1: Default Background -->
      <rect x="0" y="0" width="100%" height="100%" style="fill:url(#lgGray)" />

      <!-- Layer 2: Client Background (Poster Image) -->
      <xsl:if test="$posterimage">
        <image opacity="0.6" style="filter:url(#blur)">
          <xsl:attribute name="xlink:href">
            <xsl:value-of select="$posterimage" />
          </xsl:attribute>
          <xsl:attribute name="width">
            <xsl:value-of select="$width" />
          </xsl:attribute>
          <xsl:attribute name="height">
            <xsl:value-of select="$height" />
          </xsl:attribute>

          <filter id="blur">
            <feGaussianBlur stdDeviation="5" />
          </filter>
        </image>
      </xsl:if>

      <!-- Layer 3: Metadata -->
      <text class="metadata">
        <tspan class="title" y="30%" x="50%">
          <xsl:value-of select="opencast:XsltHelper.split(metadata/title, 30, 1, false)" />
        </tspan>
        <tspan class="title" dy="10%" x="50%">
          <xsl:value-of select="opencast:XsltHelper.split(metadata/title, 30, 2, true)" />
        </tspan>
        <tspan class="creators" dy="12%" x="50%">
          <xsl:value-of select="opencast:XsltHelper.split(metadata/creators, 40, 1, true)" />
        </tspan>
        <tspan class="description" dy="12%" x="50%">
          <xsl:value-of select="opencast:XsltHelper.split(metadata/description, 50, 1, false)" />
        </tspan>
        <tspan class="description" dy="5%" x="50%">
          <xsl:value-of select="opencast:XsltHelper.split(metadata/description, 50, 2, false)" />
        </tspan>
        <tspan class="description" dy="5%" x="50%">
          <xsl:value-of select="opencast:XsltHelper.split(metadata/description, 50, 3, true)" />
        </tspan>
        <tspan class="presentationdate" dy="12%" x="50%">
          <xsl:value-of select="date:format-date(metadata/date, 'MMMMMMMMMM dd, YYYY, HH:mm')" />
        </tspan>
        <tspan class="license" dy="10%" x="50%">
          <xsl:value-of select="metadata/license" />
        </tspan>
      </text>
    </svg>
  </xsl:template>

</xsl:stylesheet>
