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

package org.opencastproject.adminui.impl

import org.opencastproject.mediapackage.MediaPackageElementFlavor.flavor
import org.opencastproject.mediapackage.MediaPackageElementFlavor.parseFlavor

import org.opencastproject.assetmanager.api.AssetManager
import org.opencastproject.assetmanager.util.WorkflowPropertiesUtil
import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.distribution.api.DistributionException
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageElement
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Publication
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.attachment.AttachmentImpl
import org.opencastproject.publication.api.ConfigurablePublicationService
import org.opencastproject.publication.api.OaiPmhPublicationService
import org.opencastproject.publication.api.PublicationException
import org.opencastproject.security.urlsigning.exception.UrlSigningException
import org.opencastproject.security.urlsigning.service.UrlSigningService
import org.opencastproject.util.MimeType
import org.opencastproject.util.MimeTypes
import org.opencastproject.util.NotFoundException
import org.opencastproject.util.UnknownFileTypeException
import org.opencastproject.util.data.Tuple
import org.opencastproject.workflow.handler.distribution.InternalPublicationChannel
import org.opencastproject.workspace.api.Workspace

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashSet
import java.util.Optional
import java.util.OptionalDouble
import java.util.UUID
import java.util.function.Predicate
import java.util.stream.Collectors

class ThumbnailImpl(config: AdminUIConfiguration, private val workspace: Workspace,
                    private val oaiPmhPublicationService: OaiPmhPublicationService,
                    private val configurablePublicationService: ConfigurablePublicationService, private val assetManager: AssetManager,
                    private val composerService: ComposerService) {

    private val previewFlavor: MediaPackageElementFlavor
    private val masterProfile: String
    private val previewProfile: String
    private val previewProfileDownscale: String
    private val uploadedFlavor: MediaPackageElementFlavor
    private val uploadedTags: List<String>
    val defaultPosition: Double
    private val sourceFlavorSubtype: String
    private val sourceFlavorPrimary: MediaPackageElementFlavor
    private val sourceFlavorSecondary: MediaPackageElementFlavor
    private var tempThumbnailFileName: String? = null
    private val tempThumbnailId: String?
    private var tempThumbnail: URI? = null
    private var tempThumbnailMimeType: MimeType? = null

    private val distributionOaiPmh: AdminUIConfiguration.ThumbnailDistributionSettings?
    private val distributionConfigurable: AdminUIConfiguration.ThumbnailDistributionSettings?

    private val isAutoDistributionEnabled: Boolean
        get() = distributionOaiPmh!!.enabled || distributionConfigurable!!.enabled

    enum class ThumbnailSource private constructor(val number: Long) {
        DEFAULT(0),
        UPLOAD(1),
        SNAPSHOT(2);


        companion object {

            fun byNumber(number: Long): ThumbnailSource {
                return Arrays.stream(ThumbnailSource.values()).filter { v -> v.number == number }.findFirst().orElse(DEFAULT)
            }
        }
    }

    class Thumbnail(val type: ThumbnailSource, private val position: Double?, private val track: String?, val url: URI) {

        fun getPosition(): OptionalDouble {
            return if (position != null) {
                OptionalDouble.of(position)
            } else {
                OptionalDouble.empty()
            }
        }

        fun getTrack(): Optional<String> {
            return if (track != null) {
                Optional.of(track)
            } else {
                Optional.empty()
            }
        }
    }

    init {
        this.masterProfile = config.thumbnailMasterProfile
        this.previewFlavor = parseFlavor(config.thumbnailPreviewFlavor)
        this.previewProfile = config.thumbnailPreviewProfile
        this.previewProfileDownscale = config.thumbnailPreviewProfileDownscale
        this.uploadedFlavor = parseFlavor(config.thumbnailUploadedFlavor)
        this.uploadedTags = Arrays.asList(*config.thumbnailUploadedTags.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        this.defaultPosition = config.thumbnailDefaultPosition!!
        this.sourceFlavorSubtype = config.thumbnailSourceFlavorSubtype
        this.sourceFlavorPrimary = flavor(config.thumbnailSourceFlavorTypePrimary,
                config.thumbnailSourceFlavorSubtype)
        this.sourceFlavorSecondary = flavor(config.thumbnailSourceFlavorTypeSecondary,
                config.thumbnailSourceFlavorSubtype)
        this.tempThumbnail = null
        this.tempThumbnailId = null
        this.tempThumbnailMimeType = null
        this.tempThumbnailFileName = null
        this.distributionOaiPmh = config.thumbnailDistributionOaiPmh
        this.distributionConfigurable = config.thumbnailDistributionConfigurable
    }

    private fun getThumbnailPreviewForMediaPackage(mp: MediaPackage): Optional<Attachment> {
        val internalPublication = getPublication(mp, InternalPublicationChannel.CHANNEL_ID)
        return if (internalPublication.isPresent) {
            Arrays
                    .stream(internalPublication.get().attachments)
                    .filter { attachment -> previewFlavor.matches(attachment.flavor) }
                    .findFirst()
        } else {
            throw IllegalStateException("Expected internal publication, but found none for mp " + mp.identifier)
        }
    }

    @Throws(UrlSigningException::class, URISyntaxException::class)
    fun getThumbnail(mp: MediaPackage, urlSigningService: UrlSigningService,
                     expireSeconds: Long?): Optional<Thumbnail> {

        val optThumbnail = getThumbnailPreviewForMediaPackage(mp)
        if (!optThumbnail.isPresent) {
            return Optional.empty()
        }
        val thumbnail = optThumbnail.get()
        val url: URI
        if (urlSigningService.accepts(thumbnail.getURI().toString())) {
            url = URI(urlSigningService.sign(optThumbnail.get().getURI().toString(), expireSeconds, null, null))
        } else {
            url = thumbnail.getURI()
        }

        val ps = WorkflowPropertiesUtil
                .getLatestWorkflowProperties(assetManager, mp.identifier.compact())
        val source = ps.entries.stream()
                .filter { p -> ThumbnailImpl.THUMBNAIL_PROPERTY_TYPE == p.key }
                .map<String>(Function<Entry<String, String>, String> { it.value })
                .map<Long>(Function<String, Long> { java.lang.Long.parseLong(it) })
                .map<ThumbnailSource>(Function<Long, ThumbnailSource> { ThumbnailSource.byNumber(it) })
                .findAny()
                .orElse(ThumbnailSource.DEFAULT)
        val position = ps.entries.stream()
                .filter { p -> ThumbnailImpl.THUMBNAIL_PROPERTY_POSITION == p.key }
                .map<String>(Function<Entry<String, String>, String> { it.value })
                .map<Double>(Function<String, Double> { java.lang.Double.parseDouble(it) })
                .findAny().orElse(defaultPosition)
        val track = ps.entries.stream()
                .filter { p -> ThumbnailImpl.THUMBNAIL_PROPERTY_TRACK == p.key }
                .map<String>(Function<Entry<String, String>, String> { it.value })
                .findAny().orElse(null)

        return Optional.of(Thumbnail(source, position, track, url))
    }

    @Throws(IOException::class, NotFoundException::class, MediaPackageException::class, PublicationException::class, EncoderException::class, DistributionException::class)
    fun upload(mp: MediaPackage, inputStream: InputStream, contentType: String): MediaPackageElement {
        createTempThumbnail(mp, inputStream, contentType)

        val deletionUris = ArrayList<URI>(0)
        try {
            // Archive uploaded thumbnail (and remove old one)
            archive(mp)

            val trackFlavor = getPrimaryOrSecondaryTrack(mp).flavor

            val internalPublicationResult = updateInternalPublication(mp, true)
            deletionUris.add(internalPublicationResult!!.a)
            if (distributionConfigurable!!.enabled) {
                deletionUris.add(updateConfigurablePublication(mp, trackFlavor))
            }
            if (distributionOaiPmh!!.enabled) {
                deletionUris.add(updateOaiPmh(mp, trackFlavor))
            }

            assetManager.takeSnapshot(mp)

            // Set workflow settings: type = UPLOAD
            WorkflowPropertiesUtil
                    .storeProperty(assetManager, mp, THUMBNAIL_PROPERTY_TYPE, java.lang.Long.toString(ThumbnailSource.UPLOAD.number))

            return internalPublicationResult.b[0]
        } finally {
            inputStream.close()
            workspace.cleanup(mp.identifier)
            for (uri in deletionUris) {
                if (uri != null) {
                    workspace.delete(uri)
                }
            }
        }
    }

    @Throws(MediaPackageException::class)
    private fun getPrimaryOrSecondaryTrack(mp: MediaPackage): Track {

        val track = Optional.ofNullable(
                Arrays.stream(mp.getTracks(sourceFlavorPrimary)).findFirst()
                        .orElse(Arrays.stream(mp.getTracks(sourceFlavorSecondary)).findFirst()
                                .orElse(null)))

        return if (track.isPresent) {
            track.get()
        } else {
            throw MediaPackageException("Cannot find track with primary or secondary source flavor.")
        }
    }

    private fun archive(mp: MediaPackage) {
        val attachment = AttachmentImpl.fromURI(tempThumbnail)
        attachment.identifier = tempThumbnailId
        attachment.flavor = uploadedFlavor
        attachment.mimeType = this.tempThumbnailMimeType
        uploadedTags.forEach(Consumer<String> { attachment.addTag(it) })
        Arrays.stream(mp.getElementsByFlavor(uploadedFlavor)).forEach(Consumer<MediaPackageElement> { mp.remove(it) })
        mp.add(attachment)
    }

    @Throws(DistributionException::class, NotFoundException::class, IOException::class, MediaPackageException::class, PublicationException::class, EncoderException::class)
    private fun updateInternalPublication(mp: MediaPackage, downscale: Boolean): Tuple<URI, List<MediaPackageElement>>? {
        val priorFilter = { attachment -> previewFlavor.matches(attachment.flavor) }
        return if (downscale) {
            updatePublication(mp, InternalPublicationChannel.CHANNEL_ID, priorFilter, previewFlavor,
                    emptyList(), previewProfileDownscale)
        } else {
            updatePublication(mp, InternalPublicationChannel.CHANNEL_ID, priorFilter, previewFlavor,
                    emptyList())
        }
    }

    @Throws(NotFoundException::class, IOException::class, PublicationException::class, MediaPackageException::class, DistributionException::class, EncoderException::class)
    private fun updateOaiPmh(mp: MediaPackage, trackFlavor: MediaPackageElementFlavor): URI? {
        // Use OaiPmhPublicationService to re-publish thumbnail
        val oldOaiPmhPub = getPublication(mp, this.distributionOaiPmh!!.channelId)
        if (!oldOaiPmhPub.isPresent) {
            logger.debug("Thumbnail auto-distribution: No publications found for OAI-PMH publication channel {}",
                    this.distributionOaiPmh.channelId)
            return null
        } else {
            logger.debug("Thumbnail auto-distribution: Updating thumbnail of OAI-PMH publication channel {}",
                    this.distributionOaiPmh.channelId)
        }

        // We have to update the configurable publication to contain the new thumbnail as an attachment
        val configurablePublicationOpt = getPublication(mp, distributionConfigurable!!.channelId)
        val publicationsToUpdate = HashSet<Publication>()
        configurablePublicationOpt.ifPresent(Consumer<Publication> { publicationsToUpdate.add(it) })

        val publishThumbnailId = UUID.randomUUID().toString()
        val inputStream = tempInputStream()
        val publishThumbnailUri = workspace
                .put(mp.identifier.compact(), publishThumbnailId, this.tempThumbnailFileName, inputStream)
        inputStream.close()

        val publishAttachment = AttachmentImpl.fromURI(publishThumbnailUri)
        publishAttachment.identifier = UUID.randomUUID().toString()
        publishAttachment.flavor = distributionOaiPmh.flavor.applyTo(trackFlavor)
        for (tag in distributionOaiPmh.tags) {
            publishAttachment.addTag(tag)
        }
        publishAttachment.mimeType = this.tempThumbnailMimeType

        // Create downscaled thumbnails if desired
        val addElements = HashSet<Attachment>()
        if (distributionOaiPmh.profiles.size > 0) {
            addElements.addAll(downscaleAttachment(publishAttachment, *distributionOaiPmh.profiles))
        } else {
            addElements.add(publishAttachment)
        }

        val oaiPmhPub = oaiPmhPublicationService.replaceSync(
                mp, getRepositoryName(distributionOaiPmh.channelId),
                addElements, emptySet(),
                setOf(distributionOaiPmh.flavor), emptySet(),
                publicationsToUpdate, false)
        mp.remove(oldOaiPmhPub.get())
        mp.add(oaiPmhPub)
        return publishThumbnailUri
    }

    @Throws(DistributionException::class, NotFoundException::class, IOException::class, MediaPackageException::class, PublicationException::class, EncoderException::class)
    private fun updatePublication(mp: MediaPackage, channelId: String,
                                  priorFilter: Predicate<Attachment>, flavor: MediaPackageElementFlavor, tags: Collection<String>,
                                  vararg conversionProfiles: String): Tuple<URI, List<MediaPackageElement>>? {

        logger.debug("Updating thumnbail of flavor '{}' in publication channel '{}'", flavor, channelId)
        val pubOpt = getPublication(mp, channelId)
        if (!pubOpt.isPresent) {
            return null
        }
        val pub = pubOpt.get()

        val aid = UUID.randomUUID().toString()
        val inputStream = tempInputStream()
        val aUri = workspace.put(mp.identifier.compact(), aid, tempThumbnailFileName, inputStream)
        inputStream.close()
        val attachment = AttachmentImpl.fromURI(aUri)
        attachment.identifier = aid
        attachment.flavor = flavor
        tags.forEach(Consumer<String> { attachment.addTag(it) })
        attachment.mimeType = tempThumbnailMimeType
        val addElements = ArrayList<MediaPackageElement>()
        if (conversionProfiles != null && conversionProfiles.size > 0) {
            addElements.addAll(downscaleAttachment(attachment, *conversionProfiles))
        } else {
            addElements.add(attachment)
        }
        val removeElementsIds = Arrays.stream(pub.attachments).filter(priorFilter)
                .map<String>(Function<Attachment, String> { it.getIdentifier() }).collect<Set<String>, Any>(Collectors.toSet())
        val newPublication = this.configurablePublicationService.replaceSync(mp, channelId, addElements, removeElementsIds)
        mp.remove(pub)
        mp.add(newPublication)

        val newAttachmentIds = addElements.stream()
                .map<String>(Function<MediaPackageElement, String> { it.getIdentifier() })
                .collect<Set<String>, Any>(Collectors.toSet())
        return Tuple.tuple(aUri, Arrays.stream(newPublication.attachments)
                .filter { att -> newAttachmentIds.contains(att.identifier) }
                .collect<R, A>(Collectors.toList<T>()))
    }

    @Throws(DistributionException::class, EncoderException::class, MediaPackageException::class)
    private fun downscaleAttachment(attachment: Attachment, vararg conversionProfiles: String): List<Attachment> {
        // What the composer returns is not our original attachment, modified, but a new one, basically containing just
        // a URI.
        val downscaled = composerService.convertImageSync(attachment, *conversionProfiles)
        return downscaled.stream().map { a -> cloneAttachment(attachment, a.getURI()) }.collect<List<Attachment>, Any>(Collectors.toList())
    }

    @Throws(IOException::class, NotFoundException::class, MediaPackageException::class, PublicationException::class, EncoderException::class, DistributionException::class)
    private fun updateConfigurablePublication(mp: MediaPackage, trackFlavor: MediaPackageElementFlavor): URI? {
        val flavorFilter = { a -> a.flavor.matches(distributionConfigurable!!.flavor) }
        val tagsFilter = { a ->
            Arrays.asList(*distributionConfigurable!!.tags).stream()
                    .allMatch { t -> Arrays.asList<String>(*a.tags).contains(t) }
        }
        val priorFilter = flavorFilter.and(tagsFilter)
        val result = updatePublication(mp, distributionConfigurable!!.channelId, priorFilter,
                distributionConfigurable.flavor.applyTo(trackFlavor), Arrays.asList<T>(*distributionConfigurable.tags),
                distributionConfigurable.profiles)
        return result?.a
    }

    @Throws(NotFoundException::class, IOException::class)
    private fun tempInputStream(): InputStream {
        return workspace.read(tempThumbnail)
    }

    @Throws(IOException::class)
    private fun createTempThumbnail(mp: MediaPackage, inputStream: InputStream, contentType: String) {
        tempThumbnailMimeType = MimeTypes.parseMimeType(contentType)
        val filename = "uploaded_thumbnail." + tempThumbnailMimeType!!.suffix.getOrElse("unknown")
        val originalThumbnailId = UUID.randomUUID().toString()
        tempThumbnail = workspace.put(mp.identifier.compact(), originalThumbnailId, filename, inputStream)
        tempThumbnailFileName = "uploaded_thumbnail." + tempThumbnailMimeType!!.suffix.getOrElse("unknown")
    }

    private fun getPublication(mp: MediaPackage, channelId: String): Optional<Publication> {
        return Arrays.stream(mp.publications).filter { p -> p.channel.equals(channelId, ignoreCase = true) }.findAny()
    }

    @Throws(PublicationException::class, MediaPackageException::class, EncoderException::class, IOException::class, NotFoundException::class, UnknownFileTypeException::class, DistributionException::class)
    private fun chooseThumbnail(mp: MediaPackage, track: Track, position: Double): MediaPackageElement {

        val encodingProfile: String
        val downscale: Boolean
        if (isAutoDistributionEnabled) {
            /* We extract a high quality image that will be converted to various formats as required by the distribution
         channels. We do need to downscale the thumbnail preview image for the video editor in this case. */
            encodingProfile = this.masterProfile
            downscale = true
        } else {
            /* We only need the thumbnail preview image for the video editor so we use the corresponding encoding profile
         and we do not need to downscale that image */
            encodingProfile = this.previewProfile
            downscale = false
        }

        tempThumbnail = composerService.imageSync(track, encodingProfile, position)[0].getURI()
        tempThumbnailMimeType = MimeTypes.fromURI(tempThumbnail)
        tempThumbnailFileName = tempThumbnail!!.path.substring(tempThumbnail!!.path.lastIndexOf('/') + 1)

        val deletionUris = ArrayList<URI>(0)
        try {

            // Remove any uploaded thumbnails
            Arrays.stream(mp.getElementsByFlavor(uploadedFlavor)).forEach(Consumer<MediaPackageElement> { mp.remove(it) })

            val internalPublicationResult = updateInternalPublication(mp, downscale)
            deletionUris.add(internalPublicationResult!!.a)

            if (distributionConfigurable!!.enabled) {
                deletionUris.add(updateConfigurablePublication(mp, track.flavor))
            }
            if (distributionOaiPmh!!.enabled) {
                deletionUris.add(updateOaiPmh(mp, track.flavor))
            }

            assetManager.takeSnapshot(mp)

            // We return the single thumbnail preview image for the video editor here.
            return internalPublicationResult.b[0]
        } finally {
            workspace.cleanup(mp.identifier)
            for (uri in deletionUris) {
                if (uri != null) {
                    workspace.delete(uri)
                }
            }
        }
    }

    @Throws(PublicationException::class, MediaPackageException::class, EncoderException::class, IOException::class, NotFoundException::class, UnknownFileTypeException::class, DistributionException::class)
    fun chooseDefaultThumbnail(mp: MediaPackage, position: Double): MediaPackageElement {

        val result = chooseThumbnail(mp, getPrimaryOrSecondaryTrack(mp), position)

        // Set workflow settings: type = DEFAULT
        WorkflowPropertiesUtil
                .storeProperty(assetManager, mp, THUMBNAIL_PROPERTY_TYPE, java.lang.Long.toString(ThumbnailSource.DEFAULT.number))
        // We switch from double to string here because the AssetManager cannot store doubles, and we need a double value
        // in the workflow (properties)
        WorkflowPropertiesUtil
                .storeProperty(assetManager, mp, THUMBNAIL_PROPERTY_POSITION, java.lang.Double.toString(position))

        return result
    }

    @Throws(PublicationException::class, MediaPackageException::class, EncoderException::class, IOException::class, NotFoundException::class, UnknownFileTypeException::class, DistributionException::class)
    fun chooseThumbnail(mp: MediaPackage, trackFlavorType: String, position: Double): MediaPackageElement {

        val trackFlavor = flavor(trackFlavorType, sourceFlavorSubtype)
        val track = Arrays.stream<Track>(mp.getTracks(trackFlavor)).findFirst()

        if (!track.isPresent) {
            throw MediaPackageException("Cannot find stream with flavor $trackFlavor to extract thumbnail.")
        }

        val result = chooseThumbnail(mp, track.get(), position)

        // Set workflow settings: type = SNAPSHOT, position, track
        WorkflowPropertiesUtil
                .storeProperty(assetManager, mp, THUMBNAIL_PROPERTY_TYPE, java.lang.Long.toString(ThumbnailSource.SNAPSHOT.number))
        // We switch from double to string here because the AssetManager cannot store doubles, and we need a double value
        // in the workflow (properties)
        WorkflowPropertiesUtil.storeProperty(assetManager, mp, THUMBNAIL_PROPERTY_POSITION, java.lang.Double.toString(position))
        WorkflowPropertiesUtil.storeProperty(assetManager, mp, THUMBNAIL_PROPERTY_TRACK, trackFlavor.type)

        return result
    }

    private fun getRepositoryName(publicationChannelId: String): String {
        return publicationChannelId.replaceFirst(OaiPmhPublicationService.PUBLICATION_CHANNEL_PREFIX.toRegex(), "")
    }

    companion object {
        /** Name of the thumbnail type workflow property  */
        private val THUMBNAIL_PROPERTY_TYPE = "thumbnailType"
        /** Name of the thumbnail position workflow property  */
        private val THUMBNAIL_PROPERTY_POSITION = "thumbnailPosition"
        /** Name of the thumbnail track workflow property  */
        private val THUMBNAIL_PROPERTY_TRACK = "thumbnailTrack"

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(ThumbnailImpl::class.java)

        private fun cloneAttachment(attachmentToClone: Attachment, newUri: URI): Attachment {
            try {
                val result = MediaPackageElementParser
                        .getFromXml(MediaPackageElementParser.getAsXml(attachmentToClone)) as Attachment
                result.identifier = UUID.randomUUID().toString()
                result.setURI(newUri)
                return result
            } catch (e: MediaPackageException) {
                throw RuntimeException(e)
            }

        }
    }

}
