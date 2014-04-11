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

package org.opencastproject.mediapackage.track;

import org.opencastproject.mediapackage.AbstractMediaPackageElement;
import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageSerializer;
import org.opencastproject.mediapackage.Stream;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.MimeType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * This class is the base implementation for a media track, which itself is part of a media package, representing e. g.
 * the speaker video or the slide presentation movie.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "track", namespace = "http://mediapackage.opencastproject.org")
@XmlRootElement(name = "track", namespace = "http://mediapackage.opencastproject.org")
public class TrackImpl extends AbstractMediaPackageElement implements Track {

  /** Serial version UID */
  private static final long serialVersionUID = -1092781733885994038L;

  public static enum StreamingProtocol {
    HTTP,HLS,DASH,SMOOTH,MMS,RTP,RTSP,RTMP,RTMPE,HDS,PNM,PNA,ICY,BITTORENTLIVE,FILE,UNKNOWN
  }

  /** The duration in milliseconds */
  @XmlElement(name = "duration")
  protected Long duration = null;

  @XmlElement(name = "audio")
  protected List<AudioStream> audio = new ArrayList<AudioStream>();

  @XmlElement(name = "video")
  protected List<VideoStream> video = new ArrayList<VideoStream>();

  @XmlAttribute(name = "transport")
  protected StreamingProtocol transport = null;

  /** Needed by JAXB */
  public TrackImpl() {
    this.elementType = Track.TYPE;
  }

  /**
   * Creates a new track object.
   * 
   * @param flavor
   *          the track flavor
   * @param uri
   *          the track location
   * @param checksum
   *          the track checksum
   * @param mimeType
   *          the track mime type
   */
  TrackImpl(MediaPackageElementFlavor flavor, MimeType mimeType, URI uri, long size, Checksum checksum) {
    super(Type.Track, flavor, uri, size, checksum, mimeType);
  }

  /**
   * Creates a new track object for the given file and track type.
   * 
   * @param flavor
   *          the track flavor
   * @param uri
   *          the track location
   */
  TrackImpl(MediaPackageElementFlavor flavor, URI uri) {
    super(Type.Track, flavor, uri);
  }

  /**
   * Creates a new track from the given url.
   * 
   * @param uri
   *          the track location
   * @return the track
   */
  public static TrackImpl fromURI(URI uri) {
    return new TrackImpl(null, uri);
  }

  /**
   * Sets the track's duration in milliseconds.
   * 
   * @param duration
   *          the duration
   */
  public void setDuration(Long duration) {
    this.duration = duration;
  }

  /**
   * @see org.opencastproject.mediapackage.Track#getDuration()
   */
  @Override
  public Long getDuration() {
    return duration;
  }

  @Override
  public Stream[] getStreams() {
    List<Stream> streams = new ArrayList<Stream>(audio.size() + video.size());
    for (Stream s : audio)
      streams.add(s);
    for (Stream s : video)
      streams.add(s);
    return streams.toArray(new Stream[streams.size()]);
  }

  /**
   * Add a stream to the track.
   */
  public void addStream(AbstractStreamImpl stream) {
    if (stream instanceof AudioStreamImpl) {
      audio.add((AudioStreamImpl) stream);
    } else if (stream instanceof VideoStreamImpl) {
      video.add((VideoStreamImpl) stream);
    } else {
      throw new IllegalArgumentException("stream must be either audio or video");
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.Track#hasAudio()
   */
  @Override
  public boolean hasAudio() {
    return audio != null && audio.size() > 0;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.mediapackage.Track#hasVideo()
   */
  @Override
  public boolean hasVideo() {
    return video != null && video.size() > 0;
  }

  public List<AudioStream> getAudio() {
    return audio;
  }

  public void setAudio(List<AudioStream> audio) {
    this.audio = audio;
  }

  public List<VideoStream> getVideo() {
    return video;
  }

  public void setVideo(List<VideoStream> video) {
    this.video = video;
  }

  /**
   * @see org.opencastproject.mediapackage.AbstractMediaPackageElement#toManifest(org.w3c.dom.Document,
   *      MediaPackageSerializer)
   */
  @Override
  public Node toManifest(Document document, MediaPackageSerializer serializer) {
    Node node = super.toManifest(document, serializer);

    // duration
    if (duration != null && duration >= 0) {
      Node durationNode = document.createElement("duration");
      durationNode.appendChild(document.createTextNode(Long.toString(duration)));
      node.appendChild(durationNode);
    }

    for (Stream s : audio)
      node.appendChild(s.toManifest(document, serializer));
    for (Stream s : video)
      node.appendChild(s.toManifest(document, serializer));
    return node;
  }

  /**
   * This implementation returns the track's mime type.
   * 
   * @see org.opencastproject.mediapackage.Track#getDescription()
   */
  public String getDescription() {
    StringBuffer buf = new StringBuffer("");
    /*
     * todo boolean details = false; if (hasVideo()) { details = true; buf.append(videoSettings); } if (hasAudio()) {
     * String audioCodec = audioSettings.toString(); if (!hasVideo() || !audioCodec.equals(videoSettings.toString())) {
     * if (details) buf.append(" / "); details = true; buf.append(audioCodec); } } if (!details) {
     * buf.append(getMimeType()); }
     */
    return buf.toString().toLowerCase();
  }

  public void setTransport(StreamingProtocol transport) {
    this.transport = transport;
  }

  public StreamingProtocol getTransport() {
    if (transport == null) return autodetectTransport(getURI());
    return transport;
  }

  /**
   * @see java.lang.Object#clone() todo
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

  public static class Adapter extends XmlAdapter<TrackImpl, Track> {
    public TrackImpl marshal(Track mp) throws Exception {
      return (TrackImpl) mp;
    }

    public Track unmarshal(TrackImpl mp) throws Exception {
      return mp;
    }
  }

  private StreamingProtocol autodetectTransport(URI uri) {
    if (uri == null || uri.getScheme() == null) return null;
    if (uri.getScheme().toLowerCase().startsWith("http")) {
        if (uri.getFragment() == null) return StreamingProtocol.HTTP;
        else if (uri.getFragment().toLowerCase().endsWith(".m3u8")) return StreamingProtocol.HLS;
        else if (uri.getFragment().toLowerCase().endsWith(".mpd")) return StreamingProtocol.DASH;
        else if (uri.getFragment().toLowerCase().endsWith(".f4m")) return StreamingProtocol.HDS;
        else setTransport(StreamingProtocol.HTTP);
    }
    else if (uri.getScheme().toLowerCase().startsWith("rtmp")) return StreamingProtocol.RTMP;
    else if (uri.getScheme().toLowerCase().startsWith("rtmpe")) return StreamingProtocol.RTMPE;
    else if (uri.getScheme().toLowerCase().startsWith("file")) return StreamingProtocol.FILE;
    else if (uri.getScheme().toLowerCase().startsWith("rtp")) return StreamingProtocol.RTP;
    else if (uri.getScheme().toLowerCase().startsWith("rtsp")) return StreamingProtocol.RTSP;
    return StreamingProtocol.UNKNOWN;
  }

}
