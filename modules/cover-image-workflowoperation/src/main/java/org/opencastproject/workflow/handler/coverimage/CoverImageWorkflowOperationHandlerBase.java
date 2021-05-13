/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.workflow.handler.coverimage;

import org.opencastproject.coverimage.CoverImageException;
import org.opencastproject.coverimage.CoverImageService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.ConfiguredTagsAndFlavors;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Base implementation of the cover image workflow operation handler
 */
public abstract class CoverImageWorkflowOperationHandlerBase extends AbstractWorkflowOperationHandler {

  private static final String EPISODE_FLAVOR = "episodeFlavor";
  private static final String SERIES_FLAVOR = "seriesFlavor";
  private static final String COVERIMAGE_FILENAME = "coverimage.png";
  private static final String XSL_FILE_URL = "stylesheet";
  private static final String XML_METADATA = "metadata";
  private static final String WIDTH = "width";
  private static final String HEIGHT = "height";
  private static final String POSTERIMAGE_FLAVOR = "posterimage-flavor";
  private static final String POSTERIMAGE_URL = "posterimage";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CoverImageWorkflowOperationHandlerBase.class);

  /** Returns a cover image service */
  protected abstract CoverImageService getCoverImageService();

  /** Returns a workspace service */
  protected abstract Workspace getWorkspace();

  /** Returns a static metadata service */
  protected abstract StaticMetadataService getStaticMetadataService();

  /** Returns a dublin core catalog service */
  protected abstract DublinCoreCatalogService getDublinCoreCatalogService();

  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    WorkflowOperationInstance operation = workflowInstance.getCurrentOperation();

    logger.info("Cover Image Workflow started for media package '{}'", mediaPackage.getIdentifier());

    // User XML metadata from operation configuration, fallback to default metadata
    String xml = operation.getConfiguration(XML_METADATA);
    if (xml == null) {
      xml = getMetadataXml(mediaPackage, operation);
      logger.debug("Metadata was not part of operation configuration, using Dublin Core as fallback");
    }
    logger.debug("Metadata set to: {}", xml);

    String xsl = loadXsl(operation);
    logger.debug("XSL for transforming metadata to SVG loaded: {}", xsl);

    // Read image dimensions
    int width = getIntConfiguration(operation, WIDTH);
    logger.debug("Image width set to {}px", width);
    int height = getIntConfiguration(operation, HEIGHT);
    logger.debug("Image height set to {}px", height);

    // Read optional poster image flavor
    String posterImgUri = getPosterImageFileUrl(operation.getConfiguration(POSTERIMAGE_URL));
    if (posterImgUri == null) {
      posterImgUri = getPosterImageFileUrl(mediaPackage, operation.getConfiguration(POSTERIMAGE_FLAVOR));
    }
    if (posterImgUri == null) {
      logger.debug("No optional poster image set");
    } else {
      logger.debug("Poster image found at '{}'", posterImgUri);
    }

    //Get tags and flavors
    ConfiguredTagsAndFlavors tagsAndFlavors = getTagsAndFlavors(workflowInstance,
        Configuration.none, Configuration.none, Configuration.many, Configuration.one);

    // Read target flavor
    MediaPackageElementFlavor targetFlavor = tagsAndFlavors.getSingleTargetFlavor();

    Job generate;
    try {
      generate = getCoverImageService().generateCoverImage(xml, xsl, String.valueOf(width), String.valueOf(height),
              posterImgUri, targetFlavor.toString());
      logger.debug("Job for cover image generation created");

      if (!waitForStatus(generate).isSuccess()) {
        throw new WorkflowOperationException("'Cover image' job did not successfuly end");
      }

      generate = serviceRegistry.getJob(generate.getId());
      Attachment coverImage = (Attachment) MediaPackageElementParser.getFromXml(generate.getPayload());

      URI attachmentUri = getWorkspace().moveTo(coverImage.getURI(), mediaPackage.getIdentifier().toString(),
              UUID.randomUUID().toString(), COVERIMAGE_FILENAME);
      coverImage.setURI(attachmentUri);

      coverImage.setMimeType(MimeTypes.PNG);

      // Add tags
      for (String tag : tagsAndFlavors.getTargetTags()) {
        logger.trace("Tagging image with '{}'", tag);
        coverImage.addTag(tag);
      }

      mediaPackage.add(coverImage);
    } catch (MediaPackageException e) {
      throw new WorkflowOperationException(e);
    } catch (NotFoundException e) {
      throw new WorkflowOperationException(e);
    } catch (ServiceRegistryException e) {
      throw new WorkflowOperationException(e);
    } catch (CoverImageException e) {
      throw new WorkflowOperationException(e);
    } catch (IllegalArgumentException e) {
      throw new WorkflowOperationException(e);
    } catch (IOException e) {
      throw new WorkflowOperationException(e);
    }

    logger.info("Cover Image Workflow finished successfully for media package '{}' within {}ms",
            mediaPackage.getIdentifier(), generate.getQueueTime());
    return createResult(mediaPackage, Action.CONTINUE, generate.getQueueTime());
  }

  protected String getPosterImageFileUrl(MediaPackage mediaPackage, String posterimageFlavor)
          throws WorkflowOperationException {

    if (posterimageFlavor == null) {
      logger.debug("Optional configuration key '{}' not set", POSTERIMAGE_FLAVOR);
      return null;
    }

    MediaPackageElementFlavor flavor;
    try {
      flavor = MediaPackageElementFlavor.parseFlavor(posterimageFlavor);
    } catch (IllegalArgumentException e) {
      logger.warn("'{}' is not a valid flavor", posterimageFlavor);
      throw new WorkflowOperationException(e);
    }

    Attachment[] atts = mediaPackage.getAttachments(flavor);
    if (atts.length > 1) {
      logger.warn("More than one attachment with the flavor '{}' found in media package '{}'", posterimageFlavor,
              mediaPackage.getIdentifier());
      throw new WorkflowOperationException(
              "More than one attachment with the flavor'" + posterimageFlavor + "' found.");
    } else if (atts.length == 0) {
      logger.warn("No attachment with the flavor '{}' found in media package '{}'", posterimageFlavor,
              mediaPackage.getIdentifier());
      return null;
    }
    try {
      return getWorkspace().get(atts[0].getURI()).getAbsolutePath();
    } catch (NotFoundException e) {
      throw new WorkflowOperationException(e);
    } catch (IOException e) {
      throw new WorkflowOperationException(e);
    }
  }

  protected String getPosterImageFileUrl(String posterimageUrlOpt) {

    if (StringUtils.isBlank(posterimageUrlOpt)) {
      return null;
    }

    URL url = null;
    try {
      url = new URL(posterimageUrlOpt);
    } catch (Exception e) {
      logger.debug("Given poster image URI '{}' is not valid", posterimageUrlOpt);
    }

    if (url == null) {
      return null;
    }

    if ("file".equals(url.getProtocol())) {
      return url.toExternalForm();
    }

    try {
      File coverImageFile = getWorkspace().get(url.toURI());
      return coverImageFile.getPath();
    } catch (NotFoundException e) {
      logger.warn("Poster image could not be found at '{}'", url);
      return null;
    } catch (IOException e) {
      logger.warn("Error getting poster image: {}", e.getMessage());
      return null;
    } catch (URISyntaxException e) {
      logger.warn("Given URL '{}' is not a valid URI", url);
      return null;
    }
  }

  protected int getIntConfiguration(WorkflowOperationInstance operation, String key) throws WorkflowOperationException {
    String confString = operation.getConfiguration(key);
    int confValue = 0;
    if (StringUtils.isBlank(confString)) {
      throw new WorkflowOperationException("Configuration key '" + key + "' must be set");
    }
    try {
      confValue = Integer.parseInt(confString);
      if (confValue < 1) {
        throw new WorkflowOperationException("Configuration key '" + key
            + "' must be set to a valid positive integer value");
      }
    } catch (NumberFormatException e) {
      throw new WorkflowOperationException("Configuration key '" + key
          + "' must be set to a valid positive integer value");
    }
    return confValue;
  }

  protected String loadXsl(WorkflowOperationInstance operation) throws WorkflowOperationException {
    String xslUriString = operation.getConfiguration(XSL_FILE_URL);
    if (StringUtils.isBlank(xslUriString)) {
      throw new WorkflowOperationException("Configuration option '" + XSL_FILE_URL + "' must not be empty");
    }
    FileReader reader = null;
    try {
      URI xslUri = new URI(xslUriString);
      File xslFile = new File(xslUri);
      reader = new FileReader(xslFile);
      return IOUtils.toString(reader);
    } catch (FileNotFoundException e) {
      logger.warn("There is no (xsl) file at the given uri '{}': {}", xslUriString, e.getMessage());
      throw new WorkflowOperationException("There is no (XSL) file at the given URI", e);
    } catch (URISyntaxException e) {
      logger.warn("Given XSL file URI ({}) is not valid: {}", xslUriString, e.getMessage());
      throw new WorkflowOperationException("Given XSL file URI is not valid", e);
    } catch (IOException e) {
      logger.warn("Error while reading XSL file ({}): {}", xslUriString, e.getMessage());
      throw new WorkflowOperationException("Error while reading XSL file", e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  protected String getMetadataXml(MediaPackage mp, WorkflowOperationInstance operation) {
    //get specified episode/series flavor
    final String configuredEpisodeFlavor =
            Objects.toString(StringUtils.trimToNull(operation.getConfiguration(EPISODE_FLAVOR)),
                    "dublincore/episode");
    final String configuredSeriesFlavor =
            Objects.toString(StringUtils.trimToNull(operation.getConfiguration(SERIES_FLAVOR)),
                    "dublincore/series");

    MediaPackageElementFlavor episodeFlavor = MediaPackageElementFlavor.parseFlavor(configuredEpisodeFlavor);
    MediaPackageElementFlavor seriesFlavor = MediaPackageElementFlavor.parseFlavor(configuredSeriesFlavor);

    //Get episode metadata-catalog
    Catalog[] catalogs =
            mp.getCatalogs(new MediaPackageElementFlavor(episodeFlavor.getType(),
                    StringUtils.lowerCase(episodeFlavor.getSubtype())));

    //load metadata-catalog
    DublinCoreCatalog dc = DublinCoreUtil.loadDublinCore(getWorkspace(), catalogs[0]);
    Map<EName, List<DublinCoreValue>> data = dc.getValues();

    //build xml from metadata
    StringBuilder xml = new StringBuilder();
    xml.append("<metadata xmlns:dcterms=\"http://purl.org/dc/terms/\">");

    for (Map.Entry<EName, List<DublinCoreValue>> entry : data.entrySet()) {
      String currentKey = entry.getKey().getLocalName();
      switch(currentKey) {
        case "creator":
          appendXml(xml, "creators", getValuesAsString(entry));
          break;
        case "isPartOf":
          xml.append("<series>");
          //get series catalog
          Catalog[] seriesCatalogs =
                  mp.getCatalogs(new MediaPackageElementFlavor(seriesFlavor.getType(), seriesFlavor.getSubtype()));
          //get Series metadata
          DublinCoreCatalog dcSeries = DublinCoreUtil.loadDublinCore(getWorkspace(), seriesCatalogs[0]);
          Map<EName, List<DublinCoreValue>> seriesMetadata = dcSeries.getValues();
          //append series metadata
          for (Map.Entry<EName, List<DublinCoreValue>> seriesEntry : seriesMetadata.entrySet()) {
            String currentSeriesKey = seriesEntry.getKey().getLocalName();
            switch(currentSeriesKey) {
              case "created":
                String[] date = seriesEntry.getValue().get(0).getValue().split("\\.");
                appendXml(xml, "date", date[0]);
                break;
              case "contributor":
                appendXml(xml, "contributors", getValuesAsString(seriesEntry));
                break;
              default: String key = seriesEntry.getKey().getLocalName();
                appendXml(xml, key, getValuesAsString(seriesEntry));
            }
          }
          xml.append("</series>");
          break;
        case "temporal":
          String[] entries = entry.getValue().get(0).getValue().split(";");
          entries[0] = entries[0].trim().substring(6);
          entries[1] = entries[1].trim().substring(4);
          if (entries[0] != null) {
            appendXml(xml, "start", entries[0]);
          }
          if (entries[1] != null) {
            appendXml(xml, "end", entries[1]);
          }
          break;
        case "created":
          String[] date = entry.getValue().get(0).getValue().split("\\.");
          appendXml(xml, "date", date[0]);
          break;
        case "contributor":
          appendXml(xml, "contributors", getValuesAsString(entry));
          break;
        default: appendXml(xml, entry.getKey().getLocalName(), getValuesAsString(entry));
      }
    }

    xml.append("</metadata>");
    return xml.toString();
  }

  protected String getValuesAsString(Map.Entry<EName, List<DublinCoreValue>> entry) {
    List<DublinCoreValue> values = entry.getValue();
    String stringValues = "";
    try {
      stringValues += values.get(0).getValue();
      for (int i = 1; i < values.size(); i++) {
        stringValues += ", " + values.get(i).getValue();
      }
    } catch (IndexOutOfBoundsException e) {
      logger.warn("Given Key '{}' has no Entries : {}", entry.getKey(), e.getMessage());
    }
    return stringValues;
  }

  protected void appendXml(StringBuilder xml, String name, String body) {
    if (StringUtils.isBlank(body) || StringUtils.isBlank(name)) {
      return;
    }

    xml.append("<");
    xml.append(name);
    xml.append(">");

    xml.append(StringEscapeUtils.escapeXml(body));

    xml.append("</");
    xml.append(name);
    xml.append(">");
  }

}
