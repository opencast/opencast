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


package org.opencastproject.mediapackage.track

import org.opencastproject.mediapackage.AbstractMediaPackageElement
import org.opencastproject.mediapackage.AudioStream
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.MediaPackageException
import org.opencastproject.mediapackage.MediaPackageSerializer
import org.opencastproject.mediapackage.Stream
import org.opencastproject.mediapackage.Track
import org.opencastproject.mediapackage.VideoStream
import org.opencastproject.util.Checksum
import org.opencastproject.util.MimeType

import org.w3c.dom.Document
import org.w3c.dom.Node

import java.net.URI
import java.util.ArrayList

import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.adapters.XmlAdapter

/**
 * This class is the base implementation for a media track, which itself is part of a media package, representing e. g.
 * the speaker video or the slide presentation movie.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "track", namespace = "http://mediapackage.opencastproject.org")
@XmlRootElement(name = "track", namespace = "http://mediapackage.opencastproject.org")
class TrackImpl : AbstractMediaPackageElement, Track {

    /** The duration in milliseconds  */
    /**
     * @see org.opencastproject.mediapackage.Track.getDuration
     */
    /**
     * Sets the track's duration in milliseconds.
     *
     * @param duration
     * the duration
     */
    @XmlElement(name = "duration")
    override var duration: Long? = null

    @XmlElement(name = "audio")
    protected var audio: MutableList<AudioStream>? = ArrayList()

    @XmlElement(name = "video")
    protected var video: MutableList<VideoStream>? = ArrayList()

    @XmlAttribute(name = "transport")
    protected var transport: StreamingProtocol? = null

    /**
     * @see org.opencastproject.mediapackage.Track.isLive
     */
    @XmlElement(name = "live")
    override var isLive: Boolean = false

    override val streams: Array<Stream>
        get() {
            val streams = ArrayList<Stream>(audio!!.size + video!!.size)
            streams.addAll(audio!!)
            streams.addAll(video!!)
            return streams.toTypedArray<Stream>()
        }

    /**
     * This implementation returns the track's mime type.
     *
     * @see org.opencastproject.mediapackage.Track.getDescription
     */
    override/*
     * todo boolean details = false; if (hasVideo()) { details = true; buf.append(videoSettings); } if (hasAudio()) {
     * String audioCodec = audioSettings.toString(); if (!hasVideo() || !audioCodec.equals(videoSettings.toString())) {
     * if (details) buf.append(" / "); details = true; buf.append(audioCodec); } } if (!details) {
     * buf.append(getMimeType()); }
     */ val description: String
        get() {
            val buf = StringBuffer("")
            return buf.toString().toLowerCase()
        }

    enum class StreamingProtocol {
        DOWNLOAD, HLS, DASH, HDS, SMOOTH, MMS, RTP, RTSP, RTMP, RTMPE, PNM, PNA, ICY, BITTORENTLIVE, FILE, UNKNOWN
    }

    /** Needed by JAXB  */
    constructor() {
        this.elementType = Track.TYPE
    }

    /**
     * Creates a new track object.
     *
     * @param flavor
     * the track flavor
     * @param uri
     * the track location
     * @param checksum
     * the track checksum
     * @param mimeType
     * the track mime type
     */
    internal constructor(flavor: MediaPackageElementFlavor, mimeType: MimeType, uri: URI, size: Long, checksum: Checksum) : super(MediaPackageElement.Type.Track, flavor, uri, size, checksum, mimeType) {}

    /**
     * Creates a new track object for the given file and track type.
     *
     * @param flavor
     * the track flavor
     * @param uri
     * the track location
     */
    internal constructor(flavor: MediaPackageElementFlavor, uri: URI) : super(MediaPackageElement.Type.Track, flavor, uri) {}

    /**
     * Add a stream to the track.
     */
    fun addStream(stream: AbstractStreamImpl) {
        if (stream is AudioStreamImpl) {
            audio!!.add(stream)
        } else if (stream is VideoStreamImpl) {
            video!!.add(stream)
        } else {
            throw IllegalArgumentException("stream must be either audio or video")
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.Track.hasAudio
     */
    override fun hasAudio(): Boolean {
        return audio != null && audio!!.size > 0
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.Track.hasVideo
     */
    override fun hasVideo(): Boolean {
        return video != null && video!!.size > 0
    }

    fun getAudio(): List<AudioStream>? {
        return audio
    }

    fun setAudio(audio: MutableList<AudioStream>) {
        this.audio = audio
    }

    fun getVideo(): List<VideoStream>? {
        return video
    }

    fun setVideo(video: MutableList<VideoStream>) {
        this.video = video
    }

    /**
     * @see org.opencastproject.mediapackage.AbstractMediaPackageElement.toManifest
     */
    @Throws(MediaPackageException::class)
    override fun toManifest(document: Document, serializer: MediaPackageSerializer?): Node {
        val node = super.toManifest(document, serializer)

        // duration
        if (duration != null && duration >= 0) {
            val durationNode = document.createElement("duration")
            durationNode.appendChild(document.createTextNode(java.lang.Long.toString(duration!!)))
            node.appendChild(durationNode)
        }

        val liveNode = document.createElement("live")
        liveNode.appendChild(document.createTextNode(java.lang.Boolean.toString(isLive)))
        node.appendChild(liveNode)

        for (s in audio!!)
            node.appendChild(s.toManifest(document, serializer))
        for (s in video!!)
            node.appendChild(s.toManifest(document, serializer))
        return node
    }

    fun setTransport(transport: StreamingProtocol) {
        this.transport = transport
    }

    fun getTransport(): StreamingProtocol? {
        return if (transport == null) autodetectTransport(uri) else transport
    }

    /**
     * @see java.lang.Object.clone
     */
    // @Override
    // public Object clone() throws CloneNotSupportedException {
    // TrackImpl t = null;
    // try {
    // t = new TrackImpl(flavor, mimeType, new File(path, fileName), checksum);
    // t.duration = duration;
    // // todo
    // //t.audioSettings = (AudioSettings)audioSettings.clone();
    // //t.videoSettings = (VideoSettings)videoSettings.clone();
    // } catch (Exception e) {
    // throw new IllegalStateException("Illegal state while cloning track: " + t);
    // }
    // return super.clone();
    // }

    class Adapter : XmlAdapter<TrackImpl, Track>() {
        @Throws(Exception::class)
        override fun marshal(mp: Track): TrackImpl {
            return mp as TrackImpl
        }

        @Throws(Exception::class)
        override fun unmarshal(mp: TrackImpl): Track {
            return mp
        }
    }

    private fun autodetectTransport(uri: URI?): StreamingProtocol? {
        if (uri == null || uri.scheme == null) return null
        if (uri.scheme.toLowerCase().startsWith("http")) {
            if (uri.fragment == null)
                return StreamingProtocol.DOWNLOAD
            else if (uri.fragment.toLowerCase().endsWith(".m3u8"))
                return StreamingProtocol.HLS
            else if (uri.fragment.toLowerCase().endsWith(".mpd"))
                return StreamingProtocol.DASH
            else if (uri.fragment.toLowerCase().endsWith(".f4m"))
                return StreamingProtocol.HDS
            else
                setTransport(StreamingProtocol.DOWNLOAD)
        } else if (uri.scheme.toLowerCase().startsWith("rtmp"))
            return StreamingProtocol.RTMP
        else if (uri.scheme.toLowerCase().startsWith("rtmpe"))
            return StreamingProtocol.RTMPE
        else if (uri.scheme.toLowerCase().startsWith("file"))
            return StreamingProtocol.FILE
        else if (uri.scheme.toLowerCase().startsWith("rtp"))
            return StreamingProtocol.RTP
        else if (uri.scheme.toLowerCase().startsWith("rtsp")) return StreamingProtocol.RTSP
        return StreamingProtocol.UNKNOWN
    }

    companion object {

        /** Serial version UID  */
        private val serialVersionUID = -1092781733885994038L

        /**
         * Creates a new track from the given url.
         *
         * @param uri
         * the track location
         * @return the track
         */
        fun fromURI(uri: URI): TrackImpl {
            return TrackImpl(null, uri)
        }
    }

}
