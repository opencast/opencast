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

package org.opencastproject.composer.remote

import org.opencastproject.composer.api.ComposerService
import org.opencastproject.composer.api.EncoderException
import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.composer.api.EncodingProfileBuilder
import org.opencastproject.composer.api.EncodingProfileImpl
import org.opencastproject.composer.api.EncodingProfileList
import org.opencastproject.composer.api.LaidOutElement
import org.opencastproject.composer.layout.Dimension
import org.opencastproject.composer.layout.Serializer
import org.opencastproject.job.api.Job
import org.opencastproject.job.api.JobParser
import org.opencastproject.mediapackage.Attachment
import org.opencastproject.mediapackage.MediaPackageElementParser
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.Track
import org.opencastproject.serviceregistry.api.RemoteBase
import org.opencastproject.smil.entity.api.Smil
import org.opencastproject.util.data.Option

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Arrays
import java.util.Locale
import kotlin.collections.Map.Entry
import java.util.stream.Collectors

/**
 * Proxies a set of remote composer services for use as a JVM-local service. Remote services are selected at random.
 */
class ComposerServiceRemoteImpl : RemoteBase(ComposerService.JOB_TYPE), ComposerService {

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.encode
     */
    @Throws(EncoderException::class)
    override fun encode(sourceTrack: Track, profileId: String): Job {
        val post = HttpPost("/encode")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("sourceTrack", MediaPackageElementParser.getAsXml(sourceTrack)))
            params.add(BasicNameValuePair("profileId", profileId))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException("Unable to assemble a remote composer request for track $sourceTrack", e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val content = EntityUtils.toString(response.entity)
                val r = JobParser.parseJob(content)
                logger.info("Encoding job {} started on a remote composer", r.id)
                return r
            }
        } catch (e: Exception) {
            throw EncoderException("Unable to encode track $sourceTrack using a remote composer service", e)
        } finally {
            closeConnection(response)
        }
        throw EncoderException("Unable to encode track $sourceTrack using a remote composer service")
    }

    /**
     * {@inheritDoc}
     */
    @Throws(EncoderException::class)
    override fun parallelEncode(sourceTrack: Track, profileId: String): Job {
        val post = HttpPost("/parallelencode")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("sourceTrack", MediaPackageElementParser.getAsXml(sourceTrack)))
            params.add(BasicNameValuePair("profileId", profileId))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException("Unable to assemble a remote composer request for track $sourceTrack", e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val content = EntityUtils.toString(response.entity)
                val r = JobParser.parseJob(content)
                logger.info("Encoding job {} started on a remote composer", r.id)
                return r
            }
        } catch (e: Exception) {
            throw EncoderException("Unable to encode track $sourceTrack using a remote composer service", e)
        } finally {
            closeConnection(response)
        }
        throw EncoderException("Unable to encode track $sourceTrack using a remote composer service")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.trim
     */
    @Throws(EncoderException::class)
    override fun trim(sourceTrack: Track, profileId: String, start: Long, duration: Long): Job {
        val post = HttpPost("/trim")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("sourceTrack", MediaPackageElementParser.getAsXml(sourceTrack)))
            params.add(BasicNameValuePair("profileId", profileId))
            params.add(BasicNameValuePair("start", java.lang.Long.toString(start)))
            params.add(BasicNameValuePair("duration", java.lang.Long.toString(duration)))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException("Unable to assemble a remote composer request for track $sourceTrack", e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val content = EntityUtils.toString(response.entity)
                val r = JobParser.parseJob(content)
                logger.info("Trimming job {} started on a remote composer", r.id)
                return r
            }
        } catch (e: Exception) {
            throw EncoderException("Unable to trim track $sourceTrack using a remote composer service", e)
        } finally {
            closeConnection(response)
        }
        throw EncoderException("Unable to trim track $sourceTrack using a remote composer service")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.mux
     */
    @Throws(EncoderException::class)
    override fun mux(sourceVideoTrack: Track, sourceAudioTrack: Track, profileId: String): Job {
        val post = HttpPost("/mux")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("videoSourceTrack", MediaPackageElementParser.getAsXml(sourceVideoTrack)))
            params.add(BasicNameValuePair("audioSourceTrack", MediaPackageElementParser.getAsXml(sourceAudioTrack)))
            params.add(BasicNameValuePair("profileId", profileId))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException("Unable to assemble a remote composer request", e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val content = EntityUtils.toString(response.entity)
                val r = JobParser.parseJob(content)
                logger.info("Muxing job {} started on a remote composer", r.id)
                return r
            }
        } catch (e: IOException) {
            throw EncoderException(e)
        } finally {
            closeConnection(response)
        }
        throw EncoderException("Unable to mux tracks " + sourceVideoTrack + " and " + sourceAudioTrack
                + " using a remote composer")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.getProfile
     */
    override fun getProfile(profileId: String): EncodingProfile? {
        val get = HttpGet("/profile/$profileId.xml")
        var response: HttpResponse? = null
        try {
            response = getResponse(get, HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND)
            return if (response != null && response.statusLine.statusCode == HttpStatus.SC_OK) {
                EncodingProfileBuilder.getInstance().parseProfile(response.entity.content)
            } else {
                null
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        } finally {
            closeConnection(response)
        }
    }

    /**
     * {@inheritDoc}
     */
    @Throws(EncoderException::class)
    override fun image(sourceTrack: Track, profileId: String, vararg times: Double): Job {
        val post = HttpPost("/image")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("sourceTrack", MediaPackageElementParser.getAsXml(sourceTrack)))
            params.add(BasicNameValuePair("profileId", profileId))
            params.add(BasicNameValuePair("time", buildTimeArray(times)))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException(e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val r = JobParser.parseJob(response.entity.content)
                logger.info("Image extraction job {} started on a remote composer", r.id)
                return r
            }
        } catch (e: Exception) {
            throw EncoderException(e)
        } finally {
            closeConnection(response)
        }
        throw EncoderException("Unable to compose an image from track " + sourceTrack
                + " using the remote composer service proxy")
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun imageSync(sourceTrack: Track, profileId: String, vararg times: Double): List<Attachment> {
        val post = HttpPost("/imagesync")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("sourceTrack", MediaPackageElementParser.getAsXml(sourceTrack)))
            params.add(BasicNameValuePair("profileId", profileId))
            params.add(BasicNameValuePair("time", buildTimeArray(times)))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException(e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val xml = IOUtils.toString(response.entity.content, Charset.forName("utf-8"))
                return MediaPackageElementParser.getArrayFromXml(xml)
                        .stream().map { e -> e as Attachment }
                        .collect<List<Attachment>, Any>(Collectors.toList())
            }
        } catch (e: Exception) {
            throw EncoderException(e)
        } finally {
            closeConnection(response)
        }
        throw EncoderException("Unable to compose an image from track " + sourceTrack
                + " using the remote composer service proxy")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.image
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    override fun image(sourceTrack: Track, profileId: String, properties: Map<String, String>?): Job {
        val post = HttpPost("/image")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("sourceTrack", MediaPackageElementParser.getAsXml(sourceTrack)))
            params.add(BasicNameValuePair("profileId", profileId))
            if (properties != null)
                params.add(BasicNameValuePair("properties", mapToString(properties)))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException(e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val r = JobParser.parseJob(response.entity.content)
                logger.info("Image extraction job {} started on a remote composer", r.id)
                return r
            }
        } catch (e: Exception) {
            throw EncoderException(e)
        } finally {
            closeConnection(response)
        }
        throw EncoderException("Unable to compose an image from track " + sourceTrack
                + " using the remote composer service proxy")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.convertImage
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    override fun convertImage(image: Attachment, vararg profileIds: String): Job {
        val post = HttpPost("/convertimage")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("sourceImage", MediaPackageElementParser.getAsXml(image)))
            params.add(BasicNameValuePair("profileId", StringUtils.join(profileIds, ',')))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException(e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val r = JobParser.parseJob(response.entity.content)
                logger.info("Image conversion job {} started on a remote composer", r.id)
                return r
            }
        } catch (e: Exception) {
            throw EncoderException(e)
        } finally {
            closeConnection(response)
        }
        throw EncoderException("Unable to convert image at $image using the remote composer service proxy")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.convertImageSync
     */
    @Throws(EncoderException::class, MediaPackageException::class)
    override fun convertImageSync(image: Attachment, vararg profileIds: String): List<Attachment> {
        val post = HttpPost("/convertimagesync")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("sourceImage", MediaPackageElementParser.getAsXml(image)))
            params.add(BasicNameValuePair("profileIds", StringUtils.join(profileIds, ',')))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException(e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val xml = IOUtils.toString(response.entity.content, Charset.forName("utf-8"))
                return MediaPackageElementParser
                        .getArrayFromXml(xml).stream().map { a -> a as Attachment }.collect<List<Attachment>, Any>(Collectors.toList())
            }
        } catch (e: Exception) {
            throw EncoderException(e)
        } finally {
            closeConnection(response)
        }
        throw EncoderException("Unable to convert image at $image using the remote composer service proxy")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.composer.api.ComposerService.listProfiles
     */
    override fun listProfiles(): Array<EncodingProfile> {
        val get = HttpGet("/profiles.xml")
        var response: HttpResponse? = null
        try {
            response = getResponse(get)
            if (response != null) {
                val profileList = EncodingProfileBuilder.getInstance().parseProfileList(
                        response.entity.content)
                val list = profileList.profiles
                return list.toTypedArray()
            }
        } catch (e: Exception) {
            throw RuntimeException(
                    "Unable to list the encoding profiles registered with the remote composer service proxy", e)
        } finally {
            closeConnection(response)
        }
        throw RuntimeException("Unable to list the encoding profiles registered with the remote composer service proxy")
    }

    /**
     * Builds string containing times in seconds separated by comma.
     *
     * @param times
     * time array to be converted to string
     * @return string represented specified time array
     */
    protected fun buildTimeArray(times: DoubleArray): String {
        if (times.size == 0)
            return ""

        val builder = StringBuilder()
        builder.append(java.lang.Double.toString(times[0]))
        for (i in 1 until times.size) {
            builder.append(";" + java.lang.Double.toString(times[i]))
        }
        return builder.toString()
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun composite(compositeTrackSize: Dimension, upperTrack: Option<LaidOutElement<Track>>,
                           lowerTrack: LaidOutElement<Track>, watermark: Option<LaidOutElement<Attachment>>, profileId: String,
                           background: String, sourceAudioName: String): Job {
        val post = HttpPost("/composite")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("compositeSize", Serializer.json(compositeTrackSize).toJson()))
            params.add(BasicNameValuePair("lowerTrack", MediaPackageElementParser.getAsXml(lowerTrack.element)))
            params.add(BasicNameValuePair("lowerLayout", Serializer.json(lowerTrack.layout).toJson()))
            if (upperTrack.isSome) {
                params.add(BasicNameValuePair("upperTrack", MediaPackageElementParser.getAsXml(upperTrack.get()
                        .element)))
                params.add(BasicNameValuePair("upperLayout", Serializer.json(upperTrack.get().layout).toJson()))
            }

            if (watermark.isSome) {
                params.add(BasicNameValuePair("watermarkAttachment", MediaPackageElementParser.getAsXml(watermark.get()
                        .element)))
                params.add(BasicNameValuePair("watermarkLayout", Serializer.json(watermark.get().layout).toJson()))
            }
            params.add(BasicNameValuePair("profileId", profileId))
            params.add(BasicNameValuePair("background", background))
            params.add(BasicNameValuePair("sourceAudioName", sourceAudioName))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException(e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val r = JobParser.parseJob(response.entity.content)
                logger.info("Composite video job {} started on a remote composer", r.id)
                return r
            }
        } catch (e: Exception) {
            throw EncoderException(e)
        } finally {
            closeConnection(response)
        }
        if (upperTrack.isSome) {
            throw EncoderException("Unable to composite video from track " + lowerTrack.element + " and "
                    + upperTrack.get().element + " using the remote composer service proxy")
        } else {
            throw EncoderException("Unable to composite video from track " + lowerTrack.element
                    + " using the remote composer service proxy")
        }
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun concat(profileId: String, outputDimension: Dimension, sameCodec: Boolean, vararg tracks: Track): Job {
        return concat(profileId, outputDimension, -1.0f, sameCodec, *tracks)
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun concat(profileId: String, outputDimension: Dimension?, outputFrameRate: Float, sameCodec: Boolean,
                        vararg tracks: Track): Job {
        val post = HttpPost("/concat")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("profileId", profileId))
            if (outputDimension != null)
                params.add(BasicNameValuePair("outputDimension", Serializer.json(outputDimension).toJson()))
            params.add(BasicNameValuePair("outputFrameRate", String.format(Locale.US, "%f", outputFrameRate)))
            params.add(BasicNameValuePair("sourceTracks", MediaPackageElementParser.getArrayAsXml(Arrays.asList(*tracks))))
            if (sameCodec)
                params.add(BasicNameValuePair("sameCodec", "true"))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException(e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val r = JobParser.parseJob(response.entity.content)
                logger.info("Concat video job {} started on a remote composer", r.id)
                return r
            }
        } catch (e: Exception) {
            throw EncoderException(e)
        } finally {
            closeConnection(response)
        }
        throw EncoderException("Unable to concat videos from tracks " + tracks
                + " using the remote composer service proxy")
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun imageToVideo(sourceImageAttachment: Attachment, profileId: String, time: Double): Job {
        val post = HttpPost("/imagetovideo")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("sourceAttachment", MediaPackageElementParser.getAsXml(sourceImageAttachment)))
            params.add(BasicNameValuePair("profileId", profileId))
            params.add(BasicNameValuePair("time", java.lang.Double.toString(time)))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException(e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val r = JobParser.parseJob(response.entity.content)
                logger.info("Image to video converting job {} started on a remote composer", r.id)
                return r
            }
        } catch (e: Exception) {
            throw EncoderException(e)
        } finally {
            closeConnection(response)
        }
        throw EncoderException("Unable to convert an image to a video from attachment " + sourceImageAttachment
                + " using the remote composer service proxy")
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun demux(sourceTrack: Track, profileId: String): Job {
        val post = HttpPost("/demux")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("sourceTrack", MediaPackageElementParser.getAsXml(sourceTrack)))
            params.add(BasicNameValuePair("profileId", profileId))
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException("Unable to assemble a remote demux request for track $sourceTrack", e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val content = EntityUtils.toString(response.entity)
                val r = JobParser.parseJob(content)
                logger.info("Demuxing job {} started on a remote service ", r.id)
                return r
            }
        } catch (e: Exception) {
            throw EncoderException("Unable to demux track $sourceTrack using a remote composer service", e)
        } finally {
            closeConnection(response)
        }
        throw EncoderException("Unable to demux track $sourceTrack using a remote composer service")
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun processSmil(smil: Smil, trackParamGroupId: String, mediaType: String, profileIds: List<String>): Job {
        val post = HttpPost("/processsmil")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("smilAsXml", smil.toXML()))
            params.add(BasicNameValuePair("trackId", trackParamGroupId))
            params.add(BasicNameValuePair("mediaType", mediaType))
            params.add(BasicNameValuePair("profileIds", StringUtils.join(profileIds, ","))) // comma separated profiles
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException(e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val r = JobParser.parseJob(response.entity.content)
                logger.info("Concat video job {} started on a remote composer", r.id)
                return r
            }
        } catch (e: Exception) {
            throw EncoderException(e)
        } finally {
            closeConnection(response)
        }
        throw EncoderException("Unable to edit video group(" + trackParamGroupId + ") from smil " + smil
                + " using the remote composer service proxy")
    }

    @Throws(EncoderException::class, MediaPackageException::class)
    override fun multiEncode(sourceTrack: Track, profileIds: List<String>): Job {
        val post = HttpPost("/multiencode")
        try {
            val params = ArrayList<BasicNameValuePair>()
            params.add(BasicNameValuePair("sourceTrack", MediaPackageElementParser.getAsXml(sourceTrack)))
            params.add(BasicNameValuePair("profileIds", StringUtils.join(profileIds, ","))) // comma separated profiles
            post.entity = UrlEncodedFormEntity(params, "UTF-8")
        } catch (e: Exception) {
            throw EncoderException("Unable to assemble a remote demux request for track $sourceTrack", e)
        }

        var response: HttpResponse? = null
        try {
            response = getResponse(post)
            if (response != null) {
                val content = EntityUtils.toString(response.entity)
                val job = JobParser.parseJob(content)
                logger.info("Encoding job {} started on a remote multiencode", job.id)
                return job
            }
        } catch (e: Exception) {
            throw EncoderException("Unable to multiencode track $sourceTrack using a remote composer service", e)
        } finally {
            closeConnection(response)
        }
        throw EncoderException("Unable to multiencode track $sourceTrack using a remote composer service")
    }

    /**
     * Converts a Map<String></String>, String> to s key=value\n string, suitable for the properties form parameter expected by the
     * workflow rest endpoint.
     *
     * @param props
     * The map of strings
     * @return the string representation
     */
    private fun mapToString(props: Map<String, String>): String {
        val sb = StringBuilder()
        for ((key, value) in props) {
            sb.append(key)
            sb.append("=")
            sb.append(value)
            sb.append("\n")
        }
        return sb.toString()
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(ComposerServiceRemoteImpl::class.java)
    }

}
