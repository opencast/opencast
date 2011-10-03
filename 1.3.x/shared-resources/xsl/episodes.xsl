<?xml version="1.0" encoding="UTF-8"?>


<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ns2="http://search.opencastproject.org">
    <xsl:template match="/">
      <table cellspacing="0" width="100%">
        <xsl:for-each select="ns2:search-results/result">
            <tr class="search-item">
                    <td style="vertical-align: middle; text-align: center; width: 180px; height: 140px;">
                        <a class="itemtitle">
                            <xsl:attribute name="href">watch.html?id=<xsl:value-of
                                select="mediapackage/@id" /></xsl:attribute>
                                <img class="thumb" alt="">
                                    <xsl:for-each select="mediapackage/attachments/attachment">
                                        <xsl:choose>
                                            <xsl:when test="@type='presenter/search+preview'">
                                                <xsl:attribute name="src"><xsl:value-of select="url" /></xsl:attribute>
                                            </xsl:when>
                                            <xsl:when test="@type='presentation/search+preview'">
                                                <xsl:attribute name="src"><xsl:value-of select="url" /></xsl:attribute>
                                            </xsl:when>
                                        </xsl:choose>
                                    </xsl:for-each>
                                </img>
                            </a>
                        </td>
                        
                        <td style="vertical-align: top; text-align:left; padding-top: 10px; padding-right: 10px;">
                            <xsl:choose>
                                 <xsl:when test="mediapackage/media/track/mimetype[.='video/x-flv'] or mediapackage/media/track/mimetype[.='video/mp4'] or mediapackage/media/track/mimetype[.='audio/x-adpcm']">
                                    <b>
                                        <a class="itemtitle">
                                            <xsl:attribute name="href">watch.html?id=<xsl:value-of
                                                select="mediapackage/@id" /></xsl:attribute>
                                            <xsl:value-of select='substring(dcTitle, 0, 80)' />
                                            <xsl:if test='string-length(dcTitle)>80'>
                                                ...
                                            </xsl:if>
                                        </a>
                                    </b><br/>
                                    <span class="itemdesc">
                                        <xsl:if test="dcCreator!=''">
                                            by
                                            <xsl:value-of select="dcCreator" />
                                        </xsl:if>
                                    </span>
                                    <br />
                                    <div class="timeDate">
                                        <xsl:value-of select="dcCreated"/>
                                    </div>
                                    <xsl:value-of select='substring(dcDescription, 0, 200)' />
                                    <xsl:if test='string-length(dcDescription)>200'>
                                        ...
                                    </xsl:if>
                                </xsl:when>
                            <xsl:otherwise>
                                <b>
                                    <xsl:value-of select='substring(dcTitle, 0, 80)' />
                                    <xsl:if test='string-length(dcTitle)>80'>
                                        ...
                                    </xsl:if>
                                </b>

                                <xsl:if test="ns2:dcCreator!=''">
                                    by
                                    <xsl:value-of select="dcCreator" />
                                </xsl:if>
                                <br />
                              <b>The Matterhorn Media Player cannot play this media file.</b>
                <br />
                 Alternate media files that may be playable on other players may be listed in this
                <a>
                  <xsl:attribute name="href">../../search/episode.xml?id=<xsl:value-of
                    select="mediapackage/@id" /></xsl:attribute>
                  XML file
                </a>.
                            </xsl:otherwise>
                        </xsl:choose>
                    </td>
                    <td style="vertical-align: top; text-align:right; padding-top: 10px; padding-right: 10px;">
                        <xsl:value-of select="dcRightsHolder" />
                        <br />
                        <xsl:value-of select="dcContributor" />
                    </td>
          </tr>
        </xsl:for-each>
        </table>
        <div id="oc-episodes-total" style="display: none">
            <xsl:value-of select="ns2:search-results/@total" />
        </div>
        <div id="oc-episodes-limit" style="display: none">
            <xsl:value-of select="ns2:search-results/@limit" />
        </div>
        <div id="oc-episodes-offset" style="display: none">
            <xsl:value-of select="ns2:search-results/@offset" />
        </div>
    </xsl:template>
</xsl:stylesheet>
