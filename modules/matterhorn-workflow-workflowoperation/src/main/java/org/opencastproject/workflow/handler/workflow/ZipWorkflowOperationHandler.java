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
package org.opencastproject.workflow.handler;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.ZipUtil;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Produces a zipped archive of a mediapackage, places it in the archive collection, and removes the rest of the
 * mediapackage elements from both the mediapackage xml and if possible, from storage altogether.
 */
public class ZipWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ZipWorkflowOperationHandler.class);

  /** The workflow operation's property to consult to determine the collection to use to store an archive */
  public static final String ZIP_COLLECTION_PROPERTY = "zip-collection";

  /** The element flavors to include in the zip file */
  public static final String INCLUDE_FLAVORS_PROPERTY = "include-flavors";

  /** The zip archive's target element flavor */
  public static final String TARGET_FLAVOR_PROPERTY = "target-flavor";

  /** The zip archive's target tags */
  public static final String TARGET_TAGS_PROPERTY = "target-tags";

  /** The property indicating whether to apply compression to the archive */
  public static final String COMPRESS_PROPERTY = "compression";

  /** The default collection in the working file repository to store archives */
  public static final String DEFAULT_ZIP_COLLECTION = "zip";

  /** The default location to use when building an zip archive relative to the
   * storage directory */
  public static final String DEFAULT_ARCHIVE_TEMP_DIR = "archive-tmp";

  /** Key for configuring the location of the archive-temp folder */
  public static final String ARCHIVE_TEMP_DIR_CFG_KEY =
    "org.opencastproject.workflow.handler.ZipWorkflowOperationHandler.tmpdir";

  /** The default flavor to use for a mediapackage archive */
  public static final MediaPackageElementFlavor DEFAULT_ARCHIVE_FLAVOR = MediaPackageElementFlavor
          .parseFlavor("archive/zip");

  /** The temporary storage location */
  protected File tempStorageDir = null;

  /** The configuration properties */
  protected SortedMap<String, String> configurationOptions = null;

  /**
   * The workspace to use in retrieving and storing files.
   */
  protected Workspace workspace;

  /** The default no-arg constructor builds the configuration options set */
  public ZipWorkflowOperationHandler() {
    configurationOptions = new TreeMap<String, String>();
    configurationOptions.put(ZIP_COLLECTION_PROPERTY,
            "The configuration key that specifies the zip archive collection.  Defaults to " + DEFAULT_ZIP_COLLECTION);
    configurationOptions.put(COMPRESS_PROPERTY,
            "The configuration key that specifies whether to compress the zip archive.  Defaults to false.");
    configurationOptions.put(INCLUDE_FLAVORS_PROPERTY,
            "The configuration key that specifies the element flavors to include in the zipped mediapackage archive");
    configurationOptions.put(TARGET_FLAVOR_PROPERTY, "The target flavor for the zip archive element, defaulting to '"
            + DEFAULT_ARCHIVE_FLAVOR + "'");
    configurationOptions.put(TARGET_FLAVOR_PROPERTY, "The target flavor for the zip archive element, defaulting to '"
            + DEFAULT_ARCHIVE_FLAVOR + "'");
    configurationOptions.put(TARGET_TAGS_PROPERTY, "The target tags for the zip archive element");
  }

  /**
   * Sets the workspace to use.
   *
   * @param workspace
   *          the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Activate the component, generating the temporary storage directory for
   * building zip archives if necessary.
   *
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#activate(org.osgi.service.component.ComponentContext)
   */
  protected void activate(ComponentContext cc) {
    tempStorageDir = cc.getBundleContext().getProperty(ARCHIVE_TEMP_DIR_CFG_KEY) != null
      ? new File(cc.getBundleContext().getProperty(ARCHIVE_TEMP_DIR_CFG_KEY))
      : new File(cc.getBundleContext().getProperty("org.opencastproject.storage.dir"), DEFAULT_ARCHIVE_TEMP_DIR);
    if (!tempStorageDir.isDirectory()) {
      try {
        FileUtils.forceMkdir(tempStorageDir);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.AbstractWorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    if (workflowInstance == null) {
      throw new WorkflowOperationException("Invalid workflow instance");
    }

    final MediaPackage mediaPackage = workflowInstance.getMediaPackage();
    final WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();
    if (currentOperation == null) {
      throw new WorkflowOperationException("Cannot get current workflow operation");
    }

    String flavors = currentOperation.getConfiguration(INCLUDE_FLAVORS_PROPERTY);
    final List<MediaPackageElementFlavor> flavorsToZip = new ArrayList<MediaPackageElementFlavor>();
    MediaPackageElementFlavor targetFlavor = DEFAULT_ARCHIVE_FLAVOR;

    // Read the target flavor
    String targetFlavorOption = currentOperation.getConfiguration(TARGET_FLAVOR_PROPERTY);
    try {
      targetFlavor = targetFlavorOption == null ? DEFAULT_ARCHIVE_FLAVOR : MediaPackageElementFlavor.parseFlavor(targetFlavorOption);
      logger.trace("Using '{}' as the target flavor for the zip archive of recording {}", targetFlavor, mediaPackage);
    } catch (IllegalArgumentException e) {
      throw new WorkflowOperationException("Flavor '" + targetFlavorOption + "' is not valid", e);
    }

    // Read the target tags
    String targetTagsOption = StringUtils.trimToEmpty(currentOperation.getConfiguration(TARGET_TAGS_PROPERTY));
    String[] targetTags = StringUtils.split(targetTagsOption, ",");

    // If the configuration does not specify flavors, just zip them all
    if (flavors == null) {
      flavorsToZip.add(MediaPackageElementFlavor.parseFlavor("*/*"));
    } else {
      for (String flavor : asList(flavors)) {
        flavorsToZip.add(MediaPackageElementFlavor.parseFlavor(flavor));
      }
    }

    logger.info("Archiving mediapackage {} in workflow {}", mediaPackage, workflowInstance);

    String compressProperty = currentOperation.getConfiguration(COMPRESS_PROPERTY);
    boolean compress = compressProperty == null ? false : Boolean.valueOf(compressProperty);

    // Zip the contents of the mediapackage
    File zip = null;
    try {
      logger.info("Creating zipped archive of recording {}", mediaPackage);
      zip = zip(mediaPackage, flavorsToZip, compress);
    } catch (Exception e) {
      throw new WorkflowOperationException("Unable to create a zip archive from mediapackage " + mediaPackage, e);
    }

    // Get the collection for storing the archived mediapackage
    String configuredCollectionId = currentOperation.getConfiguration(ZIP_COLLECTION_PROPERTY);
    String collectionId = configuredCollectionId == null ? DEFAULT_ZIP_COLLECTION : configuredCollectionId;

    // Add the zip as an attachment to the mediapackage
    logger.info("Moving zipped archive of recording {} to the working file repository collection '{}'", mediaPackage,
            collectionId);

    InputStream in = null;
    URI uri = null;
    try {
      in = new FileInputStream(zip);
      uri = workspace.putInCollection(collectionId, mediaPackage.getIdentifier().compact() + ".zip", in);
      logger.info("Zipped archive of recording {} is available from {}", mediaPackage, uri);
    } catch (FileNotFoundException e) {
      throw new WorkflowOperationException("zip file " + zip + " not found", e);
    } catch (IOException e) {
      throw new WorkflowOperationException(e);
    } finally {
      IOUtils.closeQuietly(in);
    }

    Attachment attachment = (Attachment) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .elementFromURI(uri, Type.Attachment, targetFlavor);
    try {
      attachment.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, zip));
    } catch (IOException e) {
      throw new WorkflowOperationException(e);
    }
    attachment.setMimeType(MimeTypes.ZIP);

    // Apply the target tags
    for (String tag : targetTags) {
      attachment.addTag(tag);
      logger.trace("Tagging the archive of recording '{}' with '{}'", mediaPackage, tag);
    }
    attachment.setMimeType(MimeTypes.ZIP);

    // The zip file is safely in the archive, so it's now safe to attempt to remove the original zip
    try {
      FileUtils.forceDelete(zip);
    } catch (Exception e) {
      throw new WorkflowOperationException(e);
    }
    mediaPackage.add(attachment);

    return createResult(mediaPackage, Action.CONTINUE);
  }

  /**
   * Creates a zip archive of all elements in a mediapackage.
   *
   * @param mediaPackage
   *          the mediapackage to zip
   *
   * @return the zip file
   *
   * @throws IOException
   *           If an IO exception occurs
   * @throws NotFoundException
   *           If a file referenced in the mediapackage can not be found
   * @throws MediaPackageException
   *           If the mediapackage can not be serialized to xml
   * @throws WorkflowOperationException
   *           If the mediapackage is invalid
   */
  protected File zip(MediaPackage mediaPackage, List<MediaPackageElementFlavor> flavorsToZip, boolean compress)
          throws IOException, NotFoundException, MediaPackageException, WorkflowOperationException {

    if (mediaPackage == null) {
      throw new WorkflowOperationException("Invalid mediapackage");
    }

    // Create the temp directory
    File mediaPackageDir = new File(tempStorageDir, mediaPackage.getIdentifier().compact());
    FileUtils.forceMkdir(mediaPackageDir);

    // Link or copy each matching element's file from the workspace to the temp directory
    MediaPackageSerializer serializer = new DefaultMediaPackageSerializerImpl(mediaPackageDir);
    MediaPackage clone = (MediaPackage) mediaPackage.clone();
    for (MediaPackageElement element : clone.getElements()) {
      // remove the element if it doesn't match the flavors to zip
      boolean remove = true;
      for (MediaPackageElementFlavor flavor : flavorsToZip) {
        if (flavor.matches(element.getFlavor())) {
          remove = false;
          break;
        }
      }
      if (remove) {
        clone.remove(element);
        continue;
      }
      File elementDir = new File(mediaPackageDir, element.getIdentifier());
      FileUtils.forceMkdir(elementDir);
      File workspaceFile = workspace.get(element.getURI());
      File linkedFile = FileSupport.link(workspaceFile, new File(elementDir, workspaceFile.getName()), true);
      try {
        element.setURI(new URI(serializer.encodeURI(linkedFile.toURI())));
      } catch (URISyntaxException e) {
        throw new MediaPackageException("unable to serialize a mediapackage element", e);
      }
    }

    // Add the manifest
    FileUtils.writeStringToFile(new File(mediaPackageDir, "manifest.xml"), MediaPackageParser.getAsXml(clone), "UTF-8");

    // Zip the directory
    File zip = new File(tempStorageDir, clone.getIdentifier().compact() + ".zip");
    int compressValue = compress ? ZipUtil.DEFAULT_COMPRESSION : ZipUtil.NO_COMPRESSION;

    long startTime = System.currentTimeMillis();
    ZipUtil.zip(new File[] { mediaPackageDir }, zip, true, compressValue);
    long stopTime = System.currentTimeMillis();

    logger.debug("Zip file creation took {} seconds", (stopTime - startTime) / 1000);

    // Remove the directory
    FileUtils.forceDelete(mediaPackageDir);

    // Return the zip
    return zip;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return configurationOptions;
  }

}
