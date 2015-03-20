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
package org.opencastproject.workflow.handler.coverimage;

import org.opencastproject.coverimage.CoverImageException;
import org.opencastproject.coverimage.CoverImageService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.metadata.api.MetadataValue;
import org.opencastproject.metadata.api.StaticMetadata;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Base implementation of the cover image workflow operation handler
 */
public abstract class CoverImageWorkflowOperationHandlerBase extends AbstractWorkflowOperationHandler {

  private static final String COVERIMAGE_FILENAME = "coverimage.png";
  private static final String XSL_FILE_URL = "stylesheet";
  private static final String XML_METADATA = "metadata";
  private static final String WIDTH = "width";
  private static final String HEIGHT = "height";
  private static final String POSTERIMAGE_FLAVOR = "posterimage-flavor";
  private static final String POSTERIMAGE_URL = "posterimage";
  private static final String TARGET_FLAVOR = "target-flavor";

  /** The configuration options for this handler */
  private static final SortedMap<String, String> CONFIG_OPTIONS;

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CoverImageWorkflowOperationHandlerBase.class);

  static {
    CONFIG_OPTIONS = new TreeMap<String, String>();
    CONFIG_OPTIONS.put(XSL_FILE_URL, "URL to the XSL stylesheet");
    CONFIG_OPTIONS.put(XML_METADATA, "XML metadata");
    CONFIG_OPTIONS.put(WIDTH, "Width of the resulting cover image");
    CONFIG_OPTIONS.put(HEIGHT, "Height of the resulting cover image");
    CONFIG_OPTIONS.put(POSTERIMAGE_FLAVOR, "Poster image flavor");
    CONFIG_OPTIONS.put(POSTERIMAGE_URL, "URL to a poster image");
    CONFIG_OPTIONS.put(TARGET_FLAVOR, "Target flavor");
  }

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
      xml = getMetadataXml(mediaPackage);
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
    if (posterImgUri == null)
      posterImgUri = getPosterImageFileUrl(mediaPackage, operation.getConfiguration(POSTERIMAGE_FLAVOR));
    if (posterImgUri == null) {
      logger.debug("No optional poster image set");
    } else {
      logger.debug("Poster image found at '{}'", posterImgUri);
    }

    // Read target flavor
    String targetFlavor = operation.getConfiguration(TARGET_FLAVOR);
    if (StringUtils.isBlank(targetFlavor)) {
      logger.warn("Required configuration key '{}' is blank", TARGET_FLAVOR);
      throw new WorkflowOperationException("Configuration key '" + TARGET_FLAVOR + "' must be set");
    }
    try {
      MediaPackageElementFlavor.parseFlavor(targetFlavor);
    } catch (IllegalArgumentException e) {
      logger.warn("Given target flavor '{}' is not a valid flavor", targetFlavor);
      throw new WorkflowOperationException(e);
    }

    Job generate;
    try {
      generate = getCoverImageService().generateCoverImage(xml, xsl, String.valueOf(width), String.valueOf(height),
              posterImgUri, targetFlavor);
      logger.debug("Job for cover image generation created");

      if (!waitForStatus(generate).isSuccess()) {
        throw new WorkflowOperationException("'Cover image' job did not successfuly end");
      }

      generate = serviceRegistry.getJob(generate.getId());
      Attachment coverImage = (Attachment) MediaPackageElementParser.getFromXml(generate.getPayload());

      URI attachmentUri = getWorkspace().moveTo(coverImage.getURI(), mediaPackage.getIdentifier().compact(),
              UUID.randomUUID().toString(), COVERIMAGE_FILENAME);
      coverImage.setURI(attachmentUri);

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
      throw new WorkflowOperationException("More than one attachment with the flavor'" + posterimageFlavor + "' found.");
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

    if (StringUtils.isBlank(posterimageUrlOpt))
      return null;

    URL url = null;
    try {
      url = new URL(posterimageUrlOpt);
    } catch (Exception e) {
      logger.debug("Given poster image URI '{}' is not valid", posterimageUrlOpt);
    }

    if (url == null)
      return null;

    if ("file".equals(url.getProtocol()))
      return url.toExternalForm();

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
    if (StringUtils.isBlank(confString))
      throw new WorkflowOperationException("Configuration key '" + key + "' must be set");
    try {
      confValue = Integer.parseInt(confString);
      if (confValue < 1)
        throw new WorkflowOperationException("Configuration key '" + key
                + "' must be set to a valid positive integer value");
    } catch (NumberFormatException e) {
      throw new WorkflowOperationException("Configuration key '" + key
              + "' must be set to a valid positive integer value");
    }
    return confValue;
  }

  protected String loadXsl(WorkflowOperationInstance operation) throws WorkflowOperationException {
    String xslUriString = operation.getConfiguration(XSL_FILE_URL);
    if (StringUtils.isBlank(xslUriString))
      throw new WorkflowOperationException("Configuration option '" + XSL_FILE_URL + "' must not be empty");
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

  protected String getMetadataXml(MediaPackage mp) {
    StaticMetadata metadata = getStaticMetadataService().getMetadata(mp);

    StringBuilder xml = new StringBuilder();
    xml.append("<metadata xmlns:dcterms=\"http://purl.org/dc/terms/\">");

    for (String title : getFirstMetadataValue(metadata.getTitles()))
      appendXml(xml, "title", title);
    for (String language : metadata.getLanguage())
      appendXml(xml, "language", language);
    for (Date created : metadata.getCreated())
      appendXml(xml, "date", DateTimeSupport.toUTC(created.getTime()));
    for (String license : getFirstMetadataValue(metadata.getLicenses()))
      appendXml(xml, "license", license);
    for (String isPartOf : metadata.getIsPartOf())
      appendXml(xml, "series", isPartOf);
    for (String contributors : getFirstMetadataValue(metadata.getContributors()))
      appendXml(xml, "contributors", contributors);
    for (String creators : getFirstMetadataValue(metadata.getCreators()))
      appendXml(xml, "creators", creators);
    for (String subjects : getFirstMetadataValue(metadata.getSubjects()))
      appendXml(xml, "subjects", subjects);

    xml.append("</metadata>");

    return xml.toString();
  }

  protected Option<String> getFirstMetadataValue(List<MetadataValue<String>> list) {
    for (MetadataValue<String> data : list) {
      if (DublinCore.LANGUAGE_UNDEFINED.equals(data.getLanguage()))
        return Option.some(data.getValue());
    }
    return Option.<String> none();
  }

  protected void appendXml(StringBuilder xml, String name, String body) {
    if (StringUtils.isBlank(body) || StringUtils.isBlank(name))
      return;

    xml.append("<");
    xml.append(name);
    xml.append(">");

    xml.append(StringEscapeUtils.escapeXml(body));

    xml.append("</");
    xml.append(name);
    xml.append(">");
  }

}
