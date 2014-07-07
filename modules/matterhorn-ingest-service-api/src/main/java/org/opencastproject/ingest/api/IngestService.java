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
package org.opencastproject.ingest.api;

import org.opencastproject.job.api.JobProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.identifier.HandleException;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.WorkflowInstance;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

/**
 * Generates {@link MediaPackage}s from media, metadata, and attachments.
 */
public interface IngestService extends JobProducer {

  /**
   * Ingests the compressed mediapackage and starts the default workflow as defined by the
   * <code>org.opencastproject.workflow.default.definition</code> key, found in the system configuration.
   *
   * @param zippedMediaPackage
   *          A zipped file containing manifest, tracks, catalogs and attachments
   * @return Workflow instance.
   * @throws MediaPackageException
   *           if the mediapackage contained in the zip stream is invalid
   * @throws IOException
   *           if reading from the input stream fails
   * @throws IngestException
   *           if an unexpected error occurs
   */
  WorkflowInstance addZippedMediaPackage(InputStream zippedMediaPackage) throws MediaPackageException, IOException,
          IngestException;

  /**
   * Ingests the compressed mediapackage and starts the workflow as defined by <code>workflowDefinitionID</code>.
   *
   * @param zippedMediaPackage
   *          A zipped file containing manifest, tracks, catalogs and attachments
   * @param workflowDefinitionID
   *          workflow to be used with this media package
   * @return WorkflowInstance the workflow instance resulting from this ingested mediapackage
   * @throws MediaPackageException
   *           if the mediapackage contained in the zip stream is invalid
   * @throws IOException
   *           if reading from the input stream fails
   * @throws IngestException
   *           if an unexpected error occurs
   * @throws NotFoundException
   *           if the workflow definition was not found
   */
  WorkflowInstance addZippedMediaPackage(InputStream zippedMediaPackage, String workflowDefinitionID)
          throws MediaPackageException, IngestException, IOException, NotFoundException;

  /**
   * Ingests the compressed mediapackage and starts the workflow as defined by <code>workflowDefinitionID</code>. The
   * properties specified in <code>properties</code> will be submitted as configuration data to the workflow.
   *
   * @param zippedMediaPackage
   *          A zipped file containing manifest, tracks, catalogs and attachments
   * @param workflowDefinitionID
   *          workflow to be used with this media package
   * @param wfConfig
   *          configuration parameters for the workflow
   * @return Workflow instance.
   * @throws MediaPackageException
   *           if the mediapackage contained in the zip stream is invalid
   * @throws IOException
   *           if reading from the input stream fails
   * @throws IngestException
   *           if an unexpected error occurs
   * @throws NotFoundException
   *           if the workflow definition was not found
   */
  WorkflowInstance addZippedMediaPackage(InputStream zippedMediaPackage, String workflowDefinitionID,
          Map<String, String> wfConfig) throws MediaPackageException, IOException, IngestException, NotFoundException;

  /**
   * Ingests the compressed mediapackage and starts the workflow as defined by <code>workflowDefinitionID</code>. The
   * properties specified in <code>properties</code> will be submitted as configuration data to the workflow.
   * <p>
   * The steps defined in that workflow will be appended to the already running workflow instance
   * <code>workflowId</code>. If that workflow can't be found, a {@link NotFoundException} will be thrown. If the
   * <code>workflowId</code> is null, a new {@link WorkflowInstance} is created.
   *
   * @param zippedMediaPackage
   *          A zipped file containing manifest, tracks, catalogs and attachments
   * @param workflowDefinitionID
   *          workflow to be used with this media package
   * @param wfConfig
   *          configuration parameters for the workflow
   * @param workflowId
   *          the workflow instance
   * @return Workflow instance.
   * @throws MediaPackageException
   *           if the mediapackage contained in the zip stream is invalid
   * @throws IOException
   *           if reading from the input stream fails
   * @throws IngestException
   *           if an unexpected error occurs
   * @throws NotFoundException
   *           if either one of the workflow definition or workflow instance was not found
   * @throws UnauthorizedException
   *           if the current user does not have {@link #READ_PERMISSION} on the workflow instance's mediapackage.
   */
  WorkflowInstance addZippedMediaPackage(InputStream zippedMediaPackage, String workflowDefinitionID,
          Map<String, String> wfConfig, Long worfklowId) throws MediaPackageException, IOException, IngestException,
          NotFoundException, UnauthorizedException;

  /**
   * Create a new MediaPackage in the repository.
   *
   * @return The created MediaPackage
   * @throws MediaPackageException
   * @throws HandleException
   * @throws ConfigurationException
   * @throws IOException
   * @throws IngestException
   *           if an unexpected error occurs
   */
  MediaPackage createMediaPackage() throws MediaPackageException, ConfigurationException, HandleException, IOException,
          IngestException;

  /**
   * Add a media track to an existing MediaPackage in the repository
   *
   * @param uri
   *          The URL of the file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackageManifest The manifest of a specific Matterhorn MediaPackage element
   * @throws MediaPackageException
   * @throws IOException
   * @throws IngestException
   *           if an unexpected error occurs
   */
  MediaPackage addTrack(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, IOException, IngestException;

  /**
   * Add a media track to an existing MediaPackage in the repository
   *
   * @param mediaFile
   *          The media file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackage The updated Matterhorn MediaPackage element
   * @throws MediaPackageException
   * @throws IOException
   * @throws IngestException
   *           if an unexpected error occurs
   */
  MediaPackage addTrack(InputStream mediaFile, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, IOException, IngestException;

  /**
   * Add a [metadata catalog] to an existing MediaPackage in the repository
   *
   * @param uri
   *          The URL of the file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackage The updated Matterhorn MediaPackage element
   * @throws MediaPackageException
   * @throws IOException
   * @throws IngestException
   *           if an unexpected error occurs
   */
  MediaPackage addCatalog(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, IOException, IngestException;

  /**
   * Add a [metadata catalog] to an existing MediaPackage in the repository
   *
   * @param catalog
   *          The catalog file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackage The updated Matterhorn MediaPackage element
   * @throws MediaPackageException
   * @throws IOException
   * @throws IngestException
   *           if an unexpected error occurs
   */
  MediaPackage addCatalog(InputStream catalog, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, IOException, IngestException;

  /**
   * Add an attachment to an existing MediaPackage in the repository
   *
   * @param uri
   *          The URL of the file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackage The updated Matterhorn MediaPackage element
   * @throws MediaPackageException
   * @throws IOException
   * @throws IngestException
   *           if an unexpected error occurs
   */
  MediaPackage addAttachment(URI uri, MediaPackageElementFlavor flavor, MediaPackage mediaPackage)
          throws MediaPackageException, IOException, IngestException;

  /**
   * Add an attachment to an existing MediaPackage in the repository
   *
   * @param file
   *          The file to add
   * @param flavor
   *          The flavor of the media that is being added
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage to which Media is being added
   * @return MediaPackage The updated Matterhorn MediaPackage element
   * @throws MediaPackageException
   * @throws IOException
   * @throws IngestException
   *           if an unexpected error occurs
   */
  MediaPackage addAttachment(InputStream file, String fileName, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws MediaPackageException, IOException, IngestException;

  /**
   * Ingests the mediapackage and starts the default workflow as defined by the
   * <code>org.opencastproject.workflow.default.definition</code> key, found in the system configuration.
   *
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage being ingested
   * @return Workflow instance id.
   * @throws IngestException
   *           if an unexpected error occurs
   */
  WorkflowInstance ingest(MediaPackage mediaPackage) throws IllegalStateException, IngestException;

  /**
   * Ingests the mediapackage and starts the workflow as defined by <code>workflowDefinitionID</code>.
   *
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage being ingested
   * @param workflowDefinitionID
   *          workflow to be used with this media package
   * @return Workflow instance id.
   * @throws IngestException
   *           if an unexpected error occurs
   * @throws NotFoundException
   *           if the workflow defintion can't be found
   */
  WorkflowInstance ingest(MediaPackage mediaPackage, String workflowDefinitionID) throws IllegalStateException,
          IngestException, NotFoundException;

  /**
   * Ingests the mediapackage and starts the workflow as defined by <code>workflowDefinitionID</code>. The properties
   * specified in <code>properties</code> will be submitted as configuration data to the workflow.
   *
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage being ingested
   * @param workflowDefinitionID
   *          workflow to be used with this media package
   * @param properties
   *          configuration properties for the workflow
   * @return Workflow instance id.
   * @throws IngestException
   *           if an unexpected error occurs
   * @throws NotFoundException
   *           if the workflow defintion can't be found
   */
  WorkflowInstance ingest(MediaPackage mediaPackage, String workflowDefinitionID, Map<String, String> properties)
          throws IllegalStateException, IngestException, NotFoundException;

  /**
   * Ingests the mediapackage and starts the workflow as defined by <code>workflowDefinitionID</code>. The properties
   * specified in <code>properties</code> will be submitted as configuration data to the workflow.
   * <p>
   * The steps defined in that workflow will be appended to the already running workflow instance
   * <code>workflowId</code>. If that workflow can't be found, a {@link NotFoundException} will be thrown. If the
   * <code>workflowId</code> is null, a new {@link WorkflowInstance} is created.
   *
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage being ingested
   * @param workflowDefinitionID
   *          workflow to be used with this media package
   * @param properties
   *          configuration properties for the workflow
   * @param workflowId
   *          the workflow identifier
   * @return Workflow instance id.
   * @throws IngestException
   *           if an unexpected error occurs
   * @throws NotFoundException
   *           if either one of the workflow definition or workflow instance was not found
   * @throws UnauthorizedException
   *           if the current user does not have {@link #READ_PERMISSION} on the workflow instance's mediapackage.
   */
  WorkflowInstance ingest(MediaPackage mediaPackage, String workflowDefinitionID, Map<String, String> properties,
          Long workflowId) throws IllegalStateException, IngestException, NotFoundException, UnauthorizedException;

  /**
   * Delete an existing MediaPackage and any linked files from the temporary ingest filestore.
   *
   * @param mediaPackage
   *          The specific Matterhorn MediaPackage
   * @throws IngestException
   *           if an unexpected error occurs
   */
  void discardMediaPackage(MediaPackage mediaPackage) throws IOException, IngestException;

}
