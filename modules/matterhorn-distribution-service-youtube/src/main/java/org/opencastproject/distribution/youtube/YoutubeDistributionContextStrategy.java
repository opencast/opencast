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
package org.opencastproject.distribution.youtube;

import org.opencastproject.distribution.api.DistributionContextStragety;
import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Strategy to determine destination of distribution. Instances of this class are not threadsafe.
 */
public class YoutubeDistributionContextStrategy implements DistributionContextStragety {

  /** logger instance */
  private static final Logger logger = LoggerFactory.getLogger(YoutubeDistributionContextStrategy.class);

  /** Media package containing distribution metadata */
  private final MediaPackage mediaPackage;

  /** Dublincore metadata catalog for the episode */
  private final DublinCoreCatalog dcEpisode;

  /** Dublincore metadata catalog for the series */
  private final DublinCoreCatalog dcSeries;

  /**
   * Create a single-use strategy instance for distributing to yt
   * 
   * @param mediaPackageId
   *          the mediapackage identifier
   * @param workspace
   *          the workspace service
   * @throws DistributionException
   */
  public YoutubeDistributionContextStrategy(MediaPackage mp, Workspace workspace) throws DistributionException {
    if (mp == null) {
      throw new DistributionException("Media package is null");
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
   * @param mediaPackageId
   *          The mediapackage identifier
   * @return The context ID
   */
  public String getContextId(String mediaPackageId) {
    return dcSeries == null ? null : dcSeries.getFirst(DublinCore.PROPERTY_IDENTIFIER);
  }

  /**
   * Gets the name for a context within a distribution channel. This implementation ignores the mediaPackageId argument
   * in favor of the mediapackage passed in by the constructor.
   * 
   * @param mediaPackageId
   *          The mediapackage identifier
   * @return The playlist ID
   */
  public String getContextName(String mediaPackageId) {
    return mediaPackage.getSeriesTitle();
  }
  
  /**
   * Gets the name for a context within a distribution channel. This implementation ignores the mediaPackageId argument
   * in favor of the mediapackage passed in by the constructor.
   * 
   * @param mediaPackageId
   *          The mediapackage identifier
   * @return Context description
   */
  public String getContextDescription(String mediaPackageId) {
    //return mediaPackage.getSeriesDescription();
    return dcSeries == null ? null : dcSeries.getFirst(DublinCore.PROPERTY_DESCRIPTION);
  }

  /**
   * Gets the name for the episode of the media package
   * 
   * @param mediaPackageId
   *          The mediapackage identifier
   * @return the title of the episode
   */
  public String getEpisodeName(String mediaPackageId) {
    return dcEpisode == null ? null : dcEpisode.getFirst(DublinCore.PROPERTY_TITLE);
  }

  /**
   * Gets the description for the episode of the media package
   * 
   * @param mediaPackageId
   *          The mediapackage identifier
   * @return the description of the episode
   */
  public String getEpisodeDescription(String mediaPackageId) {
  //return dcEpisode == null ? null : dcEpisode.getFirst(DublinCore.PROPERTY_DESCRIPTION);
    String description = "";
    if (dcEpisode == null) {
      return null;
    } else {
      if (dcSeries != null) {
        description = dcSeries.getFirst(DublinCore.PROPERTY_TITLE);
      }
      String episodeDescription = dcEpisode.getFirst(DublinCore.PROPERTY_DESCRIPTION);
      String episodeLicense = dcEpisode.getFirst(DublinCore.PROPERTY_LICENSE);
      if (episodeDescription != null) {
        description += '\n' + episodeDescription;
      }
      if (episodeLicense != null) {
        description += '\n' + episodeLicense;
      }
      return description;
    }
  }

  /**
   * Gets the tags/keywords for the episode of the media package
   * 
   * @param mediaPackageId
   *          The mediapackage identifier
   * @return the keywords of the episode
   */
  public String[] getEpisodeKeywords(String mediaPackageId) {
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
   * @param workspace
   *          Workspace containing the XML of the catalog
   * @return Catalog parse from XML
   */
  private DublinCoreCatalog parseDublinCoreCatalog(Catalog catalog, Workspace workspace) {
    DublinCoreCatalog dublincore = null;
    try {
      File dcFile = workspace.get(catalog.getURI());
      InputStream is = new FileInputStream(dcFile);
      dublincore = new DublinCoreCatalogImpl(is);
      IOUtils.closeQuietly(is);
    } catch (Exception e) {
      logger.error("Error loading Dublin Core metadata: {}", e.getMessage());
    }
    return dublincore;
  }

}
