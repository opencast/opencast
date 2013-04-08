/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.publication.youtube;

import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Strategy to determine destination of publication. Instances of this class are not threadsafe.
 */
public class YouTubePublicationContextStrategy {

  /** logger instance */
  private static final Logger logger = LoggerFactory.getLogger(YouTubePublicationContextStrategy.class);

  /** Media package containing publication metadata */
  private final MediaPackage mediaPackage;

  /** Dublincore metadata catalog for the episode */
  private final DublinCoreCatalog dcEpisode;

  /** Dublincore metadata catalog for the series */
  private final DublinCoreCatalog dcSeries;

  /**
   * Create a single-use strategy instance for publication to youtube
   * 
   * @param mediaPackageId
   *          the mediapackage identifier
   * @param workspace
   *          the workspace service
   * @throws PublicationException
   */
  public YouTubePublicationContextStrategy(MediaPackage mp, Workspace workspace) throws PublicationException {
    if (mp == null) {
      throw new PublicationException("Media package is null");
    }
    mediaPackage = mp;

    Catalog[] episodeCatalogs = mediaPackage.getCatalogs(MediaPackageElements.EPISODE);
    if (episodeCatalogs.length == 0) {
      dcEpisode = null;
    } else {
      dcEpisode = parseDublinCoreCatalog(episodeCatalogs[0], workspace);
    }

    Catalog[] seriesCatalogs = mediaPackage.getCatalogs(MediaPackageElements.SERIES);
    if (seriesCatalogs.length == 0) {
      dcSeries = null;
    } else {
      dcSeries = parseDublinCoreCatalog(seriesCatalogs[0], workspace);
    }
  }

  /**
   * Returns a series identifier of the mediapackage.
   * 
   * @return The context ID
   */
  public String getContextId() {
    return dcSeries == null ? null : dcSeries.getFirst(DublinCore.PROPERTY_IDENTIFIER);
  }

  /**
   * Gets the name for a context within a publication channel.
   * 
   * @return The playlist ID
   */
  public String getContextName() {
    return mediaPackage.getSeriesTitle();
  }

  /**
   * Gets the name for a context within a publication channel.
   * 
   * @return Context description
   */
  public String getContextDescription() {
    return dcSeries == null ? null : dcSeries.getFirst(DublinCore.PROPERTY_DESCRIPTION);
  }

  /**
   * Gets the name for the episode of the media package
   * 
   * @return the title of the episode
   */
  public String getEpisodeName() {
    return dcEpisode == null ? null : dcEpisode.getFirst(DublinCore.PROPERTY_TITLE);
  }

  /**
   * Gets the description for the episode of the media package
   * 
   * @return the description of the episode
   */
  public String getEpisodeDescription() {
    if (dcEpisode == null)
      return null;

    String description = "";
    if (dcSeries != null)
      description = StringUtils.trimToEmpty(dcSeries.getFirst(DublinCore.PROPERTY_TITLE));

    String episodeDescription = dcEpisode.getFirst(DublinCore.PROPERTY_DESCRIPTION);
    if (episodeDescription != null)
      description += '\n' + episodeDescription;

    String episodeLicense = dcEpisode.getFirst(DublinCore.PROPERTY_LICENSE);
    if (episodeLicense != null)
      description += '\n' + episodeLicense;

    return description;
  }

  /**
   * Gets the tags/keywords for the episode of the media package
   * 
   * @return the keywords of the episode
   */
  public String[] getEpisodeKeywords() {
    String keywords = dcEpisode == null ? null : dcEpisode.getFirst(DublinCoreCatalog.PROPERTY_SUBJECT);
    if (keywords == null)
      return new String[0];
    else
      return keywords.split(", ");
  }

  /**
   * Parse Dublincore metadata from the workspace
   * 
   * @param catalog
   *          A mediapackage's catalog file
   * @return Catalog parse from XML
   */
  private DublinCoreCatalog parseDublinCoreCatalog(Catalog catalog, Workspace workspace) {
    InputStream is = null;
    try {
      File dcFile = workspace.get(catalog.getURI());
      is = new FileInputStream(dcFile);
      return new DublinCoreCatalogImpl(is);
    } catch (Exception e) {
      logger.error("Error loading Dublin Core metadata: {}", e.getMessage());
    } finally {
      IOUtils.closeQuietly(is);
    }
    return null;
  }

}
