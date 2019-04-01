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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.ingest.api

import org.opencastproject.job.api.JobProducer
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.scheduler.api.SchedulerException
import org.opencastproject.security.api.UnauthorizedException
import org.opencastproject.util.ConfigurationException
import org.opencastproject.util.NotFoundException
import org.opencastproject.workflow.api.WorkflowInstance

import java.io.IOException
import java.io.InputStream
import java.net.URI

/**
 * Generates [MediaPackage]s from media, metadata, and attachments.
 */
interface IngestService : JobProducer {

    /**
     * Ingests the compressed mediapackage and starts the default workflow as defined by the
     * `org.opencastproject.workflow.default.definition` key, found in the system configuration.
     *
     * @param zippedMediaPackage
     * A zipped file containing manifest, tracks, catalogs and attachments
     * @return Workflow instance.
     * @throws MediaPackageException
     * if the mediapackage contained in the zip stream is invalid
     * @throws IOException
     * if reading from the input stream fails
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class)
    fun addZippedMediaPackage(zippedMediaPackage: InputStream): WorkflowInstance

    /**
     * Ingests the compressed mediapackage and starts the workflow as defined by `workflowDefinitionID`.
     *
     * @param zippedMediaPackage
     * A zipped file containing manifest, tracks, catalogs and attachments
     * @param workflowDefinitionID
     * workflow to be used with this media package
     * @return WorkflowInstance the workflow instance resulting from this ingested mediapackage
     * @throws MediaPackageException
     * if the mediapackage contained in the zip stream is invalid
     * @throws IOException
     * if reading from the input stream fails
     * @throws IngestException
     * if an unexpected error occurs
     * @throws NotFoundException
     * if the workflow definition was not found
     */
    @Throws(MediaPackageException::class, IngestException::class, IOException::class, NotFoundException::class)
    fun addZippedMediaPackage(zippedMediaPackage: InputStream, workflowDefinitionID: String): WorkflowInstance

    /**
     * Ingests the compressed mediapackage and starts the workflow as defined by `workflowDefinitionID`. The
     * properties specified in `properties` will be submitted as configuration data to the workflow.
     *
     * @param zippedMediaPackage
     * A zipped file containing manifest, tracks, catalogs and attachments
     * @param workflowDefinitionID
     * workflow to be used with this media package
     * @param wfConfig
     * configuration parameters for the workflow
     * @return Workflow instance.
     * @throws MediaPackageException
     * if the mediapackage contained in the zip stream is invalid
     * @throws IOException
     * if reading from the input stream fails
     * @throws IngestException
     * if an unexpected error occurs
     * @throws NotFoundException
     * if the workflow definition was not found
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class, NotFoundException::class)
    fun addZippedMediaPackage(zippedMediaPackage: InputStream, workflowDefinitionID: String,
                              wfConfig: Map<String, String>): WorkflowInstance

    /**
     * Ingests the compressed mediapackage and starts the workflow as defined by `workflowDefinitionID`. The
     * properties specified in `properties` will be submitted as configuration data to the workflow.
     *
     *
     * The steps defined in that workflow will be appended to the already running workflow instance
     * `workflowId`. If that workflow can't be found, a [NotFoundException] will be thrown. If the
     * `workflowId` is null, a new [WorkflowInstance] is created.
     *
     * @param zippedMediaPackage
     * A zipped file containing manifest, tracks, catalogs and attachments
     * @param workflowDefinitionID
     * workflow to be used with this media package
     * @param wfConfig
     * configuration parameters for the workflow
     * @param workflowId
     * the workflow instance
     * @return Workflow instance.
     * @throws MediaPackageException
     * if the mediapackage contained in the zip stream is invalid
     * @throws IOException
     * if reading from the input stream fails
     * @throws IngestException
     * if an unexpected error occurs
     * @throws NotFoundException
     * if either one of the workflow definition or workflow instance was not found
     * @throws UnauthorizedException
     * if the current user does not have [org.opencastproject.security.api.Permissions.Action.READ] on the
     * workflow instance's mediapackage.
     *
     */
    @Deprecated("As of release 2.4, the scheduler service is able to store a mediapackage. Thereby the concept of the\n" +
            "                pre-procesing workflow is obsolete and there is no more need to resume such a workflow by this method.")
    @Throws(MediaPackageException::class, IOException::class, IngestException::class, NotFoundException::class, UnauthorizedException::class)
    fun addZippedMediaPackage(zippedMediaPackage: InputStream, workflowDefinitionID: String,
                              wfConfig: Map<String, String>, workflowId: Long?): WorkflowInstance

    /**
     * Create a new MediaPackage in the repository.
     *
     * @return The created MediaPackage
     * @throws MediaPackageException
     * @throws ConfigurationException
     * @throws IOException
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(MediaPackageException::class, ConfigurationException::class, IOException::class, IngestException::class)
    fun createMediaPackage(): MediaPackage

    /**
     * Create a new MediaPackage in the repository.
     * @param  mediaPackageID
     * The Id for the new Mediapackage
     * @return The created MediaPackage
     * @throws MediaPackageException
     * @throws ConfigurationException
     * @throws IOException
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(MediaPackageException::class, ConfigurationException::class, IOException::class, IngestException::class)
    fun createMediaPackage(mediaPackageID: String): MediaPackage


    /**
     * Add a media track to an existing MediaPackage in the repository
     *
     * @param uri
     * The URL of the file to add
     * @param flavor
     * The flavor of the media that is being added
     * @param mediaPackage
     * The specific Opencast MediaPackage to which Media is being added
     * @return MediaPackageManifest The manifest of a specific Opencast MediaPackage element
     * @throws MediaPackageException
     * @throws IOException
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class)
    fun addTrack(uri: URI, flavor: MediaPackageElementFlavor, mediaPackage: MediaPackage): MediaPackage

    /**
     * Add a media track to an existing MediaPackage in the repository
     *
     * @param uri
     * The URL of the file to add
     * @param flavor
     * The flavor of the media that is being added
     * @param tags
     * Tags to add to the Track
     * @param mediaPackage
     * The specific Opencast MediaPackage to which Media is being added
     * @return MediaPackageManifest The manifest of a specific Opencast MediaPackage element
     * @throws MediaPackageException
     * @throws IOException
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class)
    fun addTrack(uri: URI, flavor: MediaPackageElementFlavor, tags: Array<String>, mediaPackage: MediaPackage): MediaPackage

    /**
     * Add a media track to an existing MediaPackage in the repository
     *
     * @param mediaFile
     * The media file to add
     * @param flavor
     * The flavor of the media that is being added
     * @param mediaPackage
     * The specific Opencast MediaPackage to which Media is being added
     * @return MediaPackage The updated Opencast MediaPackage element
     * @throws MediaPackageException
     * @throws IOException
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class)
    fun addTrack(mediaFile: InputStream, fileName: String, flavor: MediaPackageElementFlavor,
                 mediaPackage: MediaPackage): MediaPackage

    /**
     * Add a media track to an existing MediaPackage in the repository
     *
     * @param mediaFile
     * The media file to add
     * @param flavor
     * The flavor of the media that is being added
     * @param tags
     * Tags to Add to the Track
     * @param mediaPackage
     * The specific Opencast MediaPackage to which Media is being added
     * @return MediaPackage The updated Opencast MediaPackage element
     * @throws MediaPackageException
     * @throws IOException
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class)
    fun addTrack(mediaFile: InputStream, fileName: String, flavor: MediaPackageElementFlavor, tags: Array<String>,
                 mediaPackage: MediaPackage): MediaPackage

    /**
     * Adds a partial media track to the existing MediaPackage in the repository
     *
     * @param uri
     * the URL of the file to add
     * @param flavor
     * the flavor of the media that is being added
     * @param startTime
     * the start time
     * @param mediaPackage
     * the mediapackage
     * @return the updated mediapackage
     * @throws IOException
     * if reading or writing of the partial track fails
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(IOException::class, IngestException::class)
    fun addPartialTrack(uri: URI, flavor: MediaPackageElementFlavor, startTime: Long, mediaPackage: MediaPackage): MediaPackage

    /**
     * Adds a partial media track to the existing MediaPackage in the repository
     *
     * @param mediaFile
     * the media file to add
     * @param fileName
     * the file name
     * @param flavor
     * the flavor of the media that is being added
     * @param startTime
     * the start time
     * @param mediaPackage
     * the mediapackage
     * @return the updated mediapackage
     * @throws IOException
     * if reading or writing of the partial track fails
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(IOException::class, IngestException::class)
    fun addPartialTrack(mediaFile: InputStream, fileName: String, flavor: MediaPackageElementFlavor,
                        startTime: Long, mediaPackage: MediaPackage): MediaPackage

    /**
     * Add a [metadata catalog] to an existing MediaPackage in the repository
     *
     * @param uri
     * The URL of the file to add
     * @param flavor
     * The flavor of the media that is being added
     * @param mediaPackage
     * The specific Opencast MediaPackage to which Media is being added
     * @return MediaPackage The updated Opencast MediaPackage element
     * @throws MediaPackageException
     * @throws IOException
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class)
    fun addCatalog(uri: URI, flavor: MediaPackageElementFlavor, mediaPackage: MediaPackage): MediaPackage

    /**
     * Add a [metadata catalog] to an existing MediaPackage in the repository
     *
     * @param catalog
     * The catalog file to add
     * @param flavor
     * The flavor of the media that is being added
     * @param mediaPackage
     * The specific Opencast MediaPackage to which Media is being added
     * @return MediaPackage The updated Opencast MediaPackage element
     * @throws MediaPackageException
     * @throws IOException
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class)
    fun addCatalog(catalog: InputStream, fileName: String, flavor: MediaPackageElementFlavor,
                   mediaPackage: MediaPackage): MediaPackage

    /**
     * Add a [metadata catalog] to an existing MediaPackage in the repository
     *
     * @param catalog
     * The catalog file to add
     * @param flavor
     * The flavor of the media that is being added
     * @param  tags
     * The tags for the media that is being added:
     * @param mediaPackage
     * The specific Opencast MediaPackage to which Media is being added
     * @return MediaPackage The updated Opencast MediaPackage element
     * @throws MediaPackageException
     * @throws IOException
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class)
    fun addCatalog(catalog: InputStream, fileName: String, flavor: MediaPackageElementFlavor, tags: Array<String>,
                   mediaPackage: MediaPackage): MediaPackage

    /**
     * Add an attachment to an existing MediaPackage in the repository
     *
     * @param uri
     * The URL of the file to add
     * @param flavor
     * The flavor of the media that is being added
     * @param mediaPackage
     * The specific Opencast MediaPackage to which Media is being added
     * @return MediaPackage The updated Opencast MediaPackage element
     * @throws MediaPackageException
     * @throws IOException
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class)
    fun addAttachment(uri: URI, flavor: MediaPackageElementFlavor, mediaPackage: MediaPackage): MediaPackage

    /**
     * Add an attachment to an existing MediaPackage in the repository
     *
     * @param file
     * The file to add
     * @param flavor
     * The flavor of the media that is being added
     * @param mediaPackage
     * The specific Opencast MediaPackage to which Media is being added
     * @return MediaPackage The updated Opencast MediaPackage element
     * @throws MediaPackageException
     * @throws IOException
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class)
    fun addAttachment(file: InputStream, fileName: String, flavor: MediaPackageElementFlavor,
                      mediaPackage: MediaPackage): MediaPackage

    /**
     * Add an attachment to an existing MediaPackage in the repository
     *
     * @param file
     * The file to add
     * @param flavor
     * The flavor of the media that is being added
     * @param tags
     * The tags of the media thas is being added
     * @param mediaPackage
     * The specific Opencast MediaPackage to which Media is being added
     * @return MediaPackage The updated Opencast MediaPackage element
     * @throws MediaPackageException
     * @throws IOException
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(MediaPackageException::class, IOException::class, IngestException::class)
    fun addAttachment(file: InputStream, fileName: String, flavor: MediaPackageElementFlavor, tags: Array<String>,
                      mediaPackage: MediaPackage): MediaPackage

    /**
     * Ingests the mediapackage and starts the default workflow as defined by the
     * `org.opencastproject.workflow.default.definition` key, found in the system configuration.
     *
     * @param mediaPackage
     * The specific Opencast MediaPackage being ingested
     * @return Workflow instance id.
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(IllegalStateException::class, IngestException::class)
    fun ingest(mediaPackage: MediaPackage): WorkflowInstance

    /**
     * Ingests the mediapackage and starts the workflow as defined by `workflowDefinitionID`.
     *
     * @param mediaPackage
     * The specific Opencast MediaPackage being ingested
     * @param workflowDefinitionID
     * workflow to be used with this media package
     * @return Workflow instance id.
     * @throws IngestException
     * if an unexpected error occurs
     * @throws NotFoundException
     * if the workflow defintion can't be found
     */
    @Throws(IllegalStateException::class, IngestException::class, NotFoundException::class)
    fun ingest(mediaPackage: MediaPackage, workflowDefinitionID: String): WorkflowInstance

    /**
     * Ingests the mediapackage and starts the workflow as defined by `workflowDefinitionID`. The properties
     * specified in `properties` will be submitted as configuration data to the workflow.
     *
     * @param mediaPackage
     * The specific Opencast MediaPackage being ingested
     * @param workflowDefinitionID
     * workflow to be used with this media package
     * @param properties
     * configuration properties for the workflow
     * @return Workflow instance id.
     * @throws IngestException
     * if an unexpected error occurs
     * @throws NotFoundException
     * if the workflow defintion can't be found
     */
    @Throws(IllegalStateException::class, IngestException::class, NotFoundException::class)
    fun ingest(mediaPackage: MediaPackage, workflowDefinitionID: String, properties: Map<String, String>): WorkflowInstance

    /**
     * Ingests the mediapackage and starts the workflow as defined by `workflowDefinitionID`. The properties
     * specified in `properties` will be submitted as configuration data to the workflow.
     *
     *
     * The steps defined in that workflow will be appended to the already running workflow instance
     * `workflowId`. If that workflow can't be found, a [NotFoundException] will be thrown. If the
     * `workflowId` is null, a new [WorkflowInstance] is created.
     *
     * @param mediaPackage
     * The specific Opencast MediaPackage being ingested
     * @param workflowDefinitionID
     * workflow to be used with this media package
     * @param properties
     * configuration properties for the workflow
     * @param workflowId
     * the workflow identifier
     * @return Workflow instance id.
     * @throws IngestException
     * if an unexpected error occurs
     * @throws NotFoundException
     * if either one of the workflow definition or workflow instance was not found
     * @throws UnauthorizedException
     * if the current user does not have [org.opencastproject.security.api.Permissions.Action.READ] on the
     * workflow instance's mediapackage.
     *
     */
    @Deprecated("As of release 2.4, the scheduler service is able to store a mediapackage. Thereby the concept of the\n" +
            "                pre-procesing workflow is obsolete and there is no more need to resume such a workflow by this method.")
    @Throws(IllegalStateException::class, IngestException::class, NotFoundException::class, UnauthorizedException::class)
    fun ingest(mediaPackage: MediaPackage, workflowDefinitionID: String, properties: Map<String, String>,
               workflowId: Long?): WorkflowInstance

    /**
     * Schedule an event with a given media package.
     *
     * @param mediaPackage
     * The specific Opencast MediaPackage being ingested
     * @param workflowDefinitionID
     * workflow to be used with this media package
     * @param properties
     * configuration properties for the workflow
     * @throws IngestException
     * if an unexpected error occurs
     * @throws NotFoundException
     * if the workflow defintion can't be found
     */
    @Throws(IllegalStateException::class, IngestException::class, NotFoundException::class, UnauthorizedException::class, SchedulerException::class)
    fun schedule(mediaPackage: MediaPackage, workflowDefinitionID: String, properties: Map<String, String>)

    /**
     * Delete an existing MediaPackage and any linked files from the temporary ingest filestore.
     *
     * @param mediaPackage
     * The specific Opencast MediaPackage
     * @throws IngestException
     * if an unexpected error occurs
     */
    @Throws(IOException::class, IngestException::class)
    fun discardMediaPackage(mediaPackage: MediaPackage)

    companion object {

        val UTC_DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'"
        val START_DATE_KEY = "ingest_start_date"
    }
}
