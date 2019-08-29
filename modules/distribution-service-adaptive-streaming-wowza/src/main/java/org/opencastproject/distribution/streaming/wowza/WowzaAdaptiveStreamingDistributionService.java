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
package org.opencastproject.distribution.streaming.wowza;

import static java.lang.String.format;
import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.distribution.api.AbstractDistributionService;
import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.StreamingDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.AudioStream;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.VideoStream;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.mediapackage.track.TrackImpl.StreamingProtocol;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.RequireUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.core.UriBuilder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Distributes media to the local media delivery directory.
 */
public class WowzaAdaptiveStreamingDistributionService extends AbstractDistributionService implements StreamingDistributionService, ManagedService {
  /** The key in the properties file that defines the streaming formats to distribute. */
  protected static final String STREAMING_FORMATS_KEY = "org.opencastproject.streaming.formats";

  /** The key in the properties file that defines the streaming url. */
  protected static final String STREAMING_URL_KEY = "org.opencastproject.streaming.url";

  /** The key in the properties file that defines the streaming port. */
  protected static final String STREAMING_PORT_KEY = "org.opencastproject.streaming.port";

  protected static final String STREAMING_DIRECTORY_KEY = "org.opencastproject.streaming.directory";

  /** The key in the properties file that defines the adaptive streaming url. */
  protected static final String ADAPTIVE_STREAMING_URL_KEY = "org.opencastproject.adaptive-streaming.url";

  /** The key in the properties file that defines the adaptive streaming port. */
  protected static final String ADAPTIVE_STREAMING_PORT_KEY = "org.opencastproject.adaptive-streaming.port";

  /** The key in the properties file that specifies in which order the videos in the SMIL file should be stored */
  protected static final String SMIL_ORDER_KEY = "org.opencastproject.adaptive-streaming.smil.order";

  /** One of the possible values for the order of the videos in the SMIL file */
  private static final String SMIL_ASCENDING_VALUE = "ascending";

  /** One of the possible values for the order of the videos in the SMIL file */
  private static final String SMIL_DESCENDING_VALUE = "descending";

  /** The attribute "video-bitrate" in the SMIL files */
  private static final String SMIL_ATTR_VIDEO_BITRATE = "video-bitrate";

  /** The attribute "video-width" in the SMIL files */
  private static final String SMIL_ATTR_VIDEO_WIDTH = "width";

  /** The attribute "video-height" in the SMIL files */
  private static final String SMIL_ATTR_VIDEO_HEIGHT = "height";

  /** The attribute to return for Distribution Type */
  private static final String DISTRIBUTION_TYPE = "streaming";

  /** Acceptable values for the streaming schemes */
  private static final Set<String> validStreamingSchemes;
  private static final Set<String> validAdaptiveStreamingSchemes;
  private static final Map<String, Integer> defaultProtocolPorts;

  static {
    Set<String> temp = new HashSet<>();
    temp.add("rtmp");
    temp.add("rtmps");
    validStreamingSchemes = Collections.unmodifiableSet(temp);

    temp = new HashSet<>();
    temp.add("http");
    temp.add("https");
    validAdaptiveStreamingSchemes = Collections.unmodifiableSet(temp);

    Map<String, Integer> tempMap = new HashMap<>();
    tempMap.put("rtmp", 1935);
    tempMap.put("rtmps", 443);
    tempMap.put("http", 80);
    tempMap.put("https", 443);
    defaultProtocolPorts = Collections.unmodifiableMap(tempMap);
  }

  /** Default scheme for streaming */
  protected static final String DEFAULT_STREAMING_SCHEME = "rtmp";

  /** Default scheme for adaptive streaming */
  protected static final String DEFAULT_ADAPTIVE_STREAMING_SCHEME = "http";

  /** Default streaming URL */
  protected static final String DEFAULT_STREAMING_URL = DEFAULT_STREAMING_SCHEME + "://localhost/matterhorn-engage";

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(WowzaAdaptiveStreamingDistributionService.class);

  /** Receipt type */
  public static final String JOB_TYPE = "org.opencastproject.distribution.streaming";

  /** List of available operations on jobs */
  private enum Operation {
    Distribute, Retract
  };

  /** The load on the system introduced by creating a distribute job */
  public static final float DEFAULT_DISTRIBUTE_JOB_LOAD = 0.1f;

  /** The load on the system introduced by creating a retract job */
  public static final float DEFAULT_RETRACT_JOB_LOAD = 0.1f;

  /** The key to look for in the service configuration file to override the {@link DEFAULT_DISTRIBUTE_JOB_LOAD} */
  public static final String DISTRIBUTE_JOB_LOAD_KEY = "job.load.streaming.distribute";

  /** The key to look for in the service configuration file to override the {@link DEFAULT_RETRACT_JOB_LOAD} */
  public static final String RETRACT_JOB_LOAD_KEY = "job.load.streaming.retract";

  /** The load on the system introduced by creating a distribute job */
  private float distributeJobLoad = DEFAULT_DISTRIBUTE_JOB_LOAD;

  /** The load on the system introduced by creating a retract job */
  private float retractJobLoad = DEFAULT_RETRACT_JOB_LOAD;

  /** The distribution directory */
  private File distributionDirectory = null;

  /** The base URI for streaming */
  protected URI streamingUri = null;

  /** The base URI for adaptive streaming */
  protected URI adaptiveStreamingUri = null;

  /** The set of supported streaming formats to distribute. */
  private Set<StreamingProtocol> supportedAdaptiveFormats;

  /** Whether or not RTMP is supported */
  private boolean isRTMPSupported = false;

  /** Whether or not the video order in the SMIL files is descending */
  private boolean isSmilOrderDescending = false;

  private static final Gson gson = new Gson();

  /**
   * Creates a new instance of the streaming distribution service.
   */
  public WowzaAdaptiveStreamingDistributionService() {
    super(JOB_TYPE);
  }

  public void activate(BundleContext bundleContext) {
    // Get the configured streaming and server URLs
    if (bundleContext != null) {

      String readStreamingUrl = StringUtils.trimToNull(bundleContext.getProperty(STREAMING_URL_KEY));
      String readStreamingPort = StringUtils.trimToNull(bundleContext.getProperty(STREAMING_PORT_KEY));
      String readAdaptiveStreamingUrl = StringUtils.trimToNull(bundleContext.getProperty(ADAPTIVE_STREAMING_URL_KEY));
      String readAdaptiveStreamingPort = StringUtils.trimToNull(bundleContext.getProperty(ADAPTIVE_STREAMING_PORT_KEY));

      try {
        streamingUri = getStreamingUrl(readStreamingUrl, readStreamingPort, validStreamingSchemes,
                DEFAULT_STREAMING_SCHEME, DEFAULT_STREAMING_URL);
        logger.info("Streaming URL set to \"{}\"", streamingUri);
      } catch (URISyntaxException e) {
        throw new ComponentException(String.format("Streaming URL %s could not be parsed", readStreamingUrl), e);
      }

      try {
        adaptiveStreamingUri = getStreamingUrl(readAdaptiveStreamingUrl, readAdaptiveStreamingPort,
                validAdaptiveStreamingSchemes, DEFAULT_ADAPTIVE_STREAMING_SCHEME, null);
        logger.info("Adaptive streaming URL set to \"{}\"", adaptiveStreamingUri);
      } catch (URISyntaxException e) {
        throw new ComponentException(
                String.format("Adaptive Streaming URL %s could not be parsed", readAdaptiveStreamingUrl), e);
      } catch (IllegalArgumentException e) {
        logger.info("Adaptive streaming URL was not defined in the configuration file");
      }

      if (adaptiveStreamingUri == null && streamingUri == null) {
        throw new ComponentException("Streaming URL and adaptive streaming URL are undefined.");
      }

      String distributionDirectoryPath = StringUtils.trimToNull(bundleContext.getProperty(STREAMING_DIRECTORY_KEY));
      if (distributionDirectoryPath == null) {
        // set default streaming directory to ${org.opencastproject.storage.dir}/streams
        distributionDirectoryPath = StringUtils.trimToNull(bundleContext.getProperty("org.opencastproject.storage.dir"));
        if (distributionDirectoryPath != null) {
          distributionDirectoryPath += "/streams";
        }
      }
      if (distributionDirectoryPath == null) {
        throw new ComponentException("Streaming distribution directory must be set");
      }

      distributionDirectory = new File(distributionDirectoryPath);
      if (!distributionDirectory.isDirectory()) {
        try {
          Files.createDirectories(distributionDirectory.toPath());
        } catch (IOException e) {
          throw new ComponentException("Distribution directory does not exist and can't be created", e);
        }
      }

      logger.info("Streaming distribution directory is {}", distributionDirectory);
    }
  }

  public String getDistributionType() {
    return DISTRIBUTION_TYPE;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    Option<String> formats;
    Option<String> smilOrder;

    if (properties == null) {
      formats = Option.none();
      smilOrder = Option.none();
    } else {
      formats = OsgiUtil.getOptCfg(properties, STREAMING_FORMATS_KEY);
      smilOrder = OsgiUtil.getOptCfg(properties, SMIL_ORDER_KEY);
    }

    if (formats.isSome()) {
      setSupportedFormats(formats.get());
    } else {
      setDefaultSupportedFormats();
    }
    logger.info("The supported streaming formats are: {}", StringUtils.join(supportedAdaptiveFormats, ","));

    if (smilOrder.isNone() || SMIL_ASCENDING_VALUE.equals(smilOrder.get())) {
      logger.info("The videos in the SMIL files will be sorted in ascending bitrate order");
      isSmilOrderDescending = false;
    } else if (SMIL_DESCENDING_VALUE.equals(smilOrder.get())) {
      isSmilOrderDescending = true;
      logger.info("The videos in the SMIL files will be sorted in descending bitrate order");
    } else {
      throw new ConfigurationException(SMIL_ORDER_KEY, format("Illegal value '%s'. Valid options are '%s' and '%s'",
              smilOrder.get(), SMIL_ASCENDING_VALUE, SMIL_DESCENDING_VALUE));
    }

    distributeJobLoad = LoadUtil.getConfiguredLoadValue(properties, DISTRIBUTE_JOB_LOAD_KEY,
            DEFAULT_DISTRIBUTE_JOB_LOAD, serviceRegistry);
    retractJobLoad = LoadUtil.getConfiguredLoadValue(properties, RETRACT_JOB_LOAD_KEY, DEFAULT_RETRACT_JOB_LOAD,
            serviceRegistry);
  }

  /**
   * Transform the configuration value into the supported formats to distribute to the Wowza server.
   *
   * @param formatString
   *          The string to parse with the supported formats.
   */
  private void setSupportedFormats(String formatString) {
    supportedAdaptiveFormats = new TreeSet<>();

    for (String format : formatString.toUpperCase().split("[\\s,]")) {
      if (!format.isEmpty()) {
        try {
          StreamingProtocol protocol = StreamingProtocol.valueOf(format);
          if (protocol.equals(StreamingProtocol.RTMP))
            isRTMPSupported = true;
          else
            supportedAdaptiveFormats.add(protocol);
        } catch (IllegalArgumentException e) {
          logger.warn("Found incorrect format \"{}\". Ignoring...", format);
        }
      }
    }
  }

  /**
   * Get the default set of supported formats to distribute to Wowza.
   */
  private void setDefaultSupportedFormats() {
    isRTMPSupported = true;
    supportedAdaptiveFormats = new TreeSet<>(Arrays.asList(
            TrackImpl.StreamingProtocol.HLS,
            TrackImpl.StreamingProtocol.HDS,
            TrackImpl.StreamingProtocol.SMOOTH,
            TrackImpl.StreamingProtocol.DASH));
  }

  /**
   * Calculate a streaming URL based on input parameters
   *
   * @throws URISyntaxException
   */
  private static URI getStreamingUrl(String inputUri, String inputPort, Set<String> validSchemes, String defaultScheme,
          String defaultUri) throws URISyntaxException {

    Integer port;
    try {
      port = Integer.parseInt(StringUtils.trimToEmpty(inputPort));
    } catch (NumberFormatException e) {
      port = null;
    }

    URI uri;
    if (StringUtils.isNotBlank(inputUri)) {
      uri = new URI(inputUri);
    } else if (StringUtils.isNotBlank(defaultUri)) {
      uri = new URI(defaultUri);
    } else {
      throw new IllegalArgumentException("Provided streaming URL is empty.");
    }
    UriBuilder uriBuilder = UriBuilder.fromUri(uri);
    String scheme = uri.getScheme();
    String uriPath = uri.getPath();
    // When a URI does not have a scheme, Java parses it as if all the URI was a (relative) path
    // However, we will assume that a host was always provided, so everything before the first "/" is the host,
    // not part of the path
    if (uri.getHost() == null) {
      uriBuilder.host(uriPath.substring(0, uriPath.indexOf("/"))).replacePath(uriPath.substring(uriPath.indexOf("/")));
    }

    if (!validSchemes.contains(scheme)) {
      if (scheme == null)
        uriBuilder.scheme(defaultScheme);
      else
        throw new URISyntaxException(inputUri, "Provided URI has an illegal scheme");
    }

    if ((port != null) && (!port.equals(defaultProtocolPorts.get(uriBuilder.build().getScheme())))) {
      uriBuilder.port(port);
    }

    return uriBuilder.build();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.distribution.api.StreamingDistributionService#distribute(java.lang.String,
   * org.opencastproject.mediapackage.MediaPackage, java.util.Set)
   */
  @Override
  public Job distribute(String channelId, MediaPackage mediapackage, Set<String> elementIds)
          throws DistributionException {

    notNull(mediapackage, "mediaPackage");
    notNull(elementIds, "elementIds");
    notNull(channelId, "channelId");

    if (streamingUri == null && adaptiveStreamingUri == null)
      throw new IllegalStateException(
              "A least one streaming url must be set (org.opencastproject.streaming.url,org.opencastproject.adaptive-streaming.url)");
    if (distributionDirectory == null)
      throw new IllegalStateException(
              "Streaming distribution directory must be set (org.opencastproject.streaming.directory)");

    try {
      return serviceRegistry.createJob(
              JOB_TYPE,
              Operation.Distribute.toString(),
              Arrays.asList(channelId, MediaPackageParser.getAsXml(mediapackage), gson.toJson(elementIds)), distributeJobLoad);
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }

  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.distribution.api.DistributionService#distribute(String,
   *      org.opencastproject.mediapackage.MediaPackage, String)
   */
  @Override
  public Job distribute(final String channelId, final MediaPackage mediapackage, final String elementId)
          throws DistributionException {
    return distribute(channelId, mediapackage, new HashSet<>(Collections.singletonList(elementId)));
  }

  /**
   * Distribute media package elements to the download distribution service.
   *
   * @param channelId The id of the publication channel to be distributed to.
   * @param mediaPackage The media package that contains the elements to be distributed.
   * @param elementIds The ids of the elements that should be distributed
   * contained within the media package.
   * @return A reference to the MediaPackageElements that have been distributed.
   * @throws DistributionException Thrown if the parent directory of the
   * MediaPackageElement cannot be created, if the MediaPackageElement cannot be
   * copied or another unexpected exception occurs.
   */
  private List<MediaPackageElement> distributeElements(final String channelId, final MediaPackage mediaPackage,
          final Set<String> elementIds) throws DistributionException {
    notNull(mediaPackage, "mediaPackage");
    notNull(elementIds, "elementIds");
    notNull(channelId, "channelId");

    List<MediaPackageElement> distributedElements = new ArrayList<>();
    for (MediaPackageElement element : getElements(mediaPackage, elementIds)) {
      distributedElements.addAll(distributeElement(channelId, mediaPackage, element));
    }
    return distributedElements;
  }

  /**
   * Distribute a media package element to the download distribution service.
   *
   * @param mediaPackage
   *          The media package that contains the element to distribute.
   * @param element
   *          The element to be distributed
   * @return A list of elements that have been distributed
   * @throws DistributionException
   *           Thrown if the parent directory of the MediaPackageElement cannot be created, if the MediaPackageElement
   *           cannot be copied or another unexpected exception occurs.
   */
  private synchronized List<MediaPackageElement> distributeElement(final String channelId,
          final MediaPackage mediaPackage, final MediaPackageElement element) throws DistributionException {

    if (!isRTMPSupported && supportedAdaptiveFormats.isEmpty()) {
      logger.warn("Skipping distribution of element \"{}\" because no streaming format was specified", element);
      return Collections.emptyList();
    }

    // Streaming servers only deal with tracks
    if (!MediaPackageElement.Type.Track.equals(element.getElementType())) {
      logger.debug("Skipping {} {} for distribution to the streaming server",
              element.getElementType(), element.getIdentifier());
      return Collections.emptyList();
    }

    try {
      File source;
      try {
        source = workspace.get(element.getURI());
      } catch (NotFoundException | IOException e) {
        throw new DistributionException("Error getting element " + element.getURI() + " from the workspace", e);
      }

      ArrayList<MediaPackageElement> distribution = new ArrayList<>();

      // Put the file in place

      File destination = getDistributionFile(channelId, mediaPackage, element);
      try {
        Files.createDirectories(destination.toPath().getParent());
      } catch (IOException e) {
        throw new DistributionException("Unable to create " + destination.getParentFile(), e);
      }
      logger.info("Distributing {} to {}", element.getIdentifier(), destination);

      try {
        FileSupport.link(source, destination, true);
      } catch (IOException e) {
        throw new DistributionException("Unable to copy " + source + " to " + destination, e);
      }

      if (isRTMPSupported) {
        // Create a representation of the distributed file in the mediaPackage
        final MediaPackageElement distributedElement = (MediaPackageElement) element.clone();

        try {
          distributedElement.setURI(getDistributionUri(channelId, mediaPackage, element));
        } catch (URISyntaxException e) {
          throw new DistributionException("Distributed element produces an invalid URI", e);
        }

        distributedElement.setIdentifier(null);
        setTransport(distributedElement, TrackImpl.StreamingProtocol.RTMP);
        distributedElement.referTo(element);

        distribution.add(distributedElement);
      }

      if ((!supportedAdaptiveFormats.isEmpty()) && isAdaptiveStreamingFormat(element)) {
        // Only if the Smil file does not exist we need to distribute adaptive streams
        // Otherwise the adaptive streams only were extended with new qualities
        File smilFile = getSmilFile(element, mediaPackage, channelId);
        Document smilXml = getSmilDocument(smilFile);
        addElementToSmil(smilXml, channelId, mediaPackage, element);
        URI smilUri = getSmilUri(smilFile);

        if (smilFile.isFile()) {
          logger.debug("Skipped adding adaptive streaming manifest {} to search index, as it already exists.", element);
        } else {
          for (StreamingProtocol protocol : supportedAdaptiveFormats) {
            distribution.add(createTrackforStreamingProtocol(element, smilUri, protocol));
            logger.info("Distributed element {} in {} format to the Wowza Server", element, protocol);
          }
        }

        saveSmilFile(smilFile, smilXml);
      }

      logger.info("Distributed file {} to Wowza Server", element);
      return distribution;

    } catch (URISyntaxException e) {
      throw new DistributionException("Error distributing " + element, e);
    }
  }

  private void setTransport(MediaPackageElement element, TrackImpl.StreamingProtocol protocol) {
    if (element instanceof TrackImpl) {
      ((TrackImpl) element).setTransport(protocol);
    }
  }

  private File getSmilFile(MediaPackageElement element, MediaPackage mediapackage, String channelId) {
    String orgId = securityService.getOrganization().getId();
    String smilFileName = channelId + "_" + mediapackage.getIdentifier() + "_" + element.getFlavor().getType()
            + ".smil";
    return distributionDirectory.toPath().resolve(Paths.get(orgId, smilFileName)).toFile();
  }

  private URI getSmilUri(File smilFile) throws URISyntaxException {
    return UriBuilder.fromUri(adaptiveStreamingUri).path("smil:" + smilFile.getName()).build();
  }

  private URI getAdaptiveStreamingUri(URI smilUri, StreamingProtocol protocol) throws URISyntaxException {
    String fileName;
    switch (protocol) {
      case HLS:
        fileName = "playlist.m3u8";
        break;
      case HDS:
        fileName = "manifest.f4m";
        break;
      case SMOOTH:
        fileName = "Manifest";
        break;
      case DASH:
        fileName = "manifest_mpm4sav_mvlist.mpd";
        break;
      default:
        fileName = "";
    }
    return new URI(UrlSupport.concat(smilUri.toString(), fileName));
  }

  private boolean isAdaptiveStreamingFormat(MediaPackageElement element) {
    String uriPath = element.getURI().getPath();
    return uriPath.endsWith(".mp4") || uriPath.contains("mp4:");
  }

  private Document getSmilDocument(File smilFile) throws DistributionException {
    if (!smilFile.isFile()) {
      try {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element smil = doc.createElement("smil");
        doc.appendChild(smil);

        Element head = doc.createElement("head");
        smil.appendChild(head);

        Element body = doc.createElement("body");
        smil.appendChild(body);

        Element switchElement = doc.createElement("switch");
        body.appendChild(switchElement);

        return doc;
      } catch (ParserConfigurationException ex) {
        logger.error("Could not create XML file for {}.", smilFile);
        throw new DistributionException("Could not create XML file for " + smilFile);
      }
    }

    try {
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(smilFile);

      if (!"smil".equalsIgnoreCase(doc.getDocumentElement().getNodeName())) {
        logger.error("XML-File {} is not a SMIL file.", smilFile);
        throw new DistributionException(format("XML-File %s is not an SMIL file.", smilFile.getName()));
      }

      return doc;
    } catch (IOException e) {
      logger.error("Could not open SMIL file {}", smilFile);
      throw new DistributionException(format("Could not open SMIL file %s", smilFile));
    } catch (ParserConfigurationException e) {
      logger.error("Could not parse SMIL file {}", smilFile);
      throw new DistributionException(format("Could not parse SMIL file %s", smilFile));
    } catch (SAXException e) {
      logger.error("Could not parse XML file {}", smilFile);
      throw new DistributionException(format("Could not parse XML file %s", smilFile));
    }
  }

  private void saveSmilFile(File smilFile, Document doc) throws DistributionException {
    try {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult stream = new StreamResult(smilFile);
      transformer.transform(source, stream);
      logger.info("SMIL file for Wowza server saved at {}", smilFile);
    } catch (TransformerException ex) {
      logger.error("Could not write SMIL file {} for distribution", smilFile);
      throw new DistributionException(format("Could not write SMIL file %s for distribution", smilFile));
    }
  }

  private void addElementToSmil(Document doc, String channelId, MediaPackage mediapackage, MediaPackageElement element)
          throws DOMException, URISyntaxException {
    if (!(element instanceof TrackImpl))
      return;
    TrackImpl track = (TrackImpl) element;
    NodeList switchElementsList = doc.getElementsByTagName("switch");
    Node switchElement = null;

    // There should only be one switch element in the file. If there are more we will igore this.
    // If there is no switch element we need to create the xml first.
    if (switchElementsList.getLength() > 0) {
      switchElement = switchElementsList.item(0);
    } else {
      if (doc.getElementsByTagName("head").getLength() < 1)
        doc.appendChild(doc.createElement("head"));
      if (doc.getElementsByTagName("body").getLength() < 1)
        doc.appendChild(doc.createElement("body"));
      switchElement = doc.createElement("switch");
      doc.getElementsByTagName("body").item(0).appendChild(switchElement);
    }

    Element video = doc.createElement("video");
    video.setAttribute("src", getAdaptiveDistributionName(channelId, mediapackage, element));

    float bitrate = 0;

    // Add bitrate corresponding to the audio streams
    for (AudioStream stream : track.getAudio()) {
      bitrate += stream.getBitRate();
    }

    // Add bitrate corresponding to the video streams
    // Also, set the video width and height values:
    // In the rare case where there is more than one video stream, the values of the first stream
    // have priority, but always prefer the first stream with both "frameWidth" and "frameHeight"
    // parameters defined
    Integer width = null;
    Integer height = null;
    for (VideoStream stream : track.getVideo()) {
      bitrate += stream.getBitRate();
      // Update if both width and height are defined for a stream or if we have no values at all
      if (((stream.getFrameWidth() != null) && (stream.getFrameHeight() != null))
              || ((width == null) && (height == null))) {
        width = stream.getFrameWidth();
        height = stream.getFrameHeight();
      }
    }

    video.setAttribute(SMIL_ATTR_VIDEO_BITRATE, Integer.toString((int) bitrate));

    if (width != null) {
      video.setAttribute(SMIL_ATTR_VIDEO_WIDTH, Integer.toString(width));
    } else {
      logger.debug("Could not set video width in the SMIL file for element {} of mediapackage {}. The value was null",
              element.getIdentifier(), mediapackage.getIdentifier());
    }
    if (height != null) {
      video.setAttribute(SMIL_ATTR_VIDEO_HEIGHT, Integer.toString(height));
    } else {
      logger.debug("Could not set video height in the SMIL file for element {} of mediapackage {}. The value was null",
              element.getIdentifier(), mediapackage.getIdentifier());
    }

    NodeList currentVideos = switchElement.getChildNodes();
    for (int i = 0; i < currentVideos.getLength(); i++) {
      Node current = currentVideos.item(i);
      if ("video".equals(current.getNodeName())) {
        float currentBitrate = Float
                .parseFloat(current.getAttributes().getNamedItem(SMIL_ATTR_VIDEO_BITRATE).getTextContent());
        if ((isSmilOrderDescending && (currentBitrate < bitrate))
                || (!isSmilOrderDescending && (currentBitrate > bitrate))) {
          switchElement.insertBefore(video, current);
          return;
        }
      }
    }

    // If we get here, we could not insert the video before
    switchElement.appendChild(video);
  }

  private TrackImpl createTrackforStreamingProtocol(MediaPackageElement element, URI smilUri,
          StreamingProtocol protocol) throws URISyntaxException {
    TrackImpl track = (TrackImpl) element.clone();

    switch (protocol) {
      case HLS:
        track.setMimeType(MimeType.mimeType("application", "x-mpegURL"));
        break;
      case HDS:
        track.setMimeType(MimeType.mimeType("application", "f4m+xml"));
        break;
      case SMOOTH:
        track.setMimeType(MimeType.mimeType("application", "vnd.ms-sstr+xml"));
        break;
      case DASH:
        track.setMimeType(MimeType.mimeType("application", "dash+xml"));
        break;
      default:
        throw new IllegalArgumentException(format("Received invalid, non-adaptive streaming protocol: '%s'", protocol));
    }

    setTransport(track, protocol);
    track.setURI(getAdaptiveStreamingUri(smilUri, protocol));
    track.referTo(element);
    track.setIdentifier(null);
    track.setAudio(null);
    track.setVideo(null);
    track.setChecksum(null);

    return track;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.distribution.api.DistributionService#retract(String,
   *      org.opencastproject.mediapackage.MediaPackage, String) java.lang.String)
   */
  @Override
  public Job retract(String channelId, MediaPackage mediapackage, String elementId) throws DistributionException {
    return retract(channelId, mediapackage, new HashSet<>(Collections.singletonList(elementId)));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.distribution.api.StreamingDistributionService#retract(java.lang.String,
   * org.opencastproject.mediapackage.MediaPackage, java.util.Set)
   */
  @Override
  public Job retract(String channelId, MediaPackage mediaPackage, Set<String> elementIds) throws DistributionException {
    RequireUtil.notNull(mediaPackage, "mediaPackage");
    RequireUtil.notNull(elementIds, "elementIds");
    RequireUtil.notNull(channelId, "channelId");
    //
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Retract.toString(),
              Arrays.asList(channelId, MediaPackageParser.getAsXml(mediaPackage), gson.toJson(elementIds)),
              retractJobLoad);
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  @Override
  public List<MediaPackageElement> distributeSync(String channelId, MediaPackage mediaPackage, Set<String> elementIds)
      throws DistributionException {
    return distributeElements(channelId, mediaPackage, elementIds);
  }

  @Override
  public List<MediaPackageElement> retractSync(String channelId, MediaPackage mediaPackage, Set<String> elementIds)
      throws DistributionException {
    return retractElements(channelId, mediaPackage, elementIds);
  }

  /**
   * Retract a media package element from the distribution channel. The retracted element must not necessarily be the
   * one given as parameter <code>elementId</code>. Instead, the element's distribution URI will be calculated. This way
   * you are able to retract elements by providing the "original" element here.
   *
   * @param channelId
   *          the channel id
   * @param mediaPackage
   *          the mediaPackage
   * @param elementIds
   *          the element identifiers
   * @return the retracted element or <code>null</code> if the element was not retracted
   * @throws org.opencastproject.distribution.api.DistributionException
   *           in case of an error
   */
  private List<MediaPackageElement> retractElements(String channelId, MediaPackage mediaPackage,
          Set<String> elementIds)
          throws DistributionException {

    if (distributionDirectory == null || (streamingUri == null || adaptiveStreamingUri == null)) {
      logger.warn("Invalid configuration");
      return Collections.emptyList();
    }

    notNull(mediaPackage, "mediaPackage");
    notNull(elementIds, "elementIds");
    notNull(channelId, "channelId");

    List<MediaPackageElement> retractedElements = new ArrayList<>();
    for (MediaPackageElement element: getElements(mediaPackage, elementIds)) {
      retractedElements.addAll(retractElement(channelId, mediaPackage, element));
    }
    return retractedElements;
  }

  /**
   * Retracts the media package with the given identifier from the distribution channel.
   *
   * @param channelId
   *          the channel id
   * @param mediaPackage
   *          the media package to retract the element from
   * @param element
   *          the element to retract
   * @return the retracted element or <code>null</code> if the element was not retracted
   */
  private List<MediaPackageElement> retractElement(final String channelId, final MediaPackage mediaPackage,
          final MediaPackageElement element) throws DistributionException {

    logger.debug("Retracting element {} with URI {}", element.getIdentifier(), element.getURI());

    // Has this element been distributed?
    if (!(element instanceof TrackImpl)) {
      return Collections.emptyList();
    }

    // Get the distribution path on the disk for this mediaPackage element
    final File elementFile = getDistributionFile(channelId, mediaPackage, element);
    final File smilFile = getSmilFile(element, mediaPackage, channelId);
    logger.debug("Deleting file {}", elementFile);

    // Does the file exist? If not, the current element has not been distributed to this channel
    // or has been removed otherwise
    if (elementFile == null || !elementFile.exists()) {
      logger.warn("{} does not exist but was to be deleted", elementFile);
      return Collections.singletonList(element);
    }

    // If a SMIL file is referenced by this element, delete first all the elements within
    if (elementFile.equals(smilFile)) {
      Document smilXml = getSmilDocument(smilFile);
      NodeList videoList = smilXml.getElementsByTagName("video");
      for (int i = 0; i < videoList.getLength(); i++) {
        if (videoList.item(i) instanceof Element) {
          String smilPathStr = ((Element) videoList.item(i)).getAttribute("src");
          // Patch the streaming tags
          if (smilPathStr.contains("mp4:"))
            smilPathStr = smilPathStr.replace("mp4:", "");
          if (!smilPathStr.endsWith(".mp4"))
            smilPathStr += ".mp4";

          deleteElementFile(smilFile.toPath().resolveSibling(smilPathStr).toFile());
        }
      }

      if (smilFile.isFile() && !smilFile.delete()) {
        logger.warn("The SMIL file {} could not be successfully deleted.", smilFile);
      }
    } else {
      deleteElementFile(elementFile);
    }

    logger.info("Finished retracting element {} of media package {}", element, mediaPackage);
    return Collections.singletonList(element);
  }

  /**
   * Delete an element file and the parent folders, if necessary
   *
   * @param elementFile
   */
  private void deleteElementFile(File elementFile) {

    // Try to remove the element file
    if (elementFile.exists()) {
      if (!elementFile.delete())
        logger.warn("Could not properly delete element file: {}", elementFile);
    } else {
      logger.warn("Tried to delete non-existent element file. Perhaps was already deleted?: {}", elementFile);
    }

    // Try to remove the parent folders, if possible
    File elementDir = elementFile.getParentFile();
    if (elementDir != null && elementDir.exists()) {
      if (elementDir.list().length == 0) {
        if (!elementDir.delete())
          logger.warn("Could not properly delete element directory: {}", elementDir);
      } else {
        logger.warn("Element directory was not empty after deleting element. Skipping deletion: {}", elementDir);
      }
    } else {
      logger.warn("Element directory did not exist when trying to delete it: {}", elementDir);
    }

    File mediapackageDir = elementDir.getParentFile();
    if (mediapackageDir != null && mediapackageDir.exists()) {
      if (mediapackageDir.list().length == 0) {
        if (!mediapackageDir.delete())
          logger.warn("Could not properly delete mediapackage directory: {}", mediapackageDir);
      } else {
        logger.debug("Mediapackage directory was not empty after deleting element. Skipping deletion: {}",
                mediapackageDir);
      }
    } else {
      logger.warn("Mediapackage directory did not exist when trying to delete it: {}", mediapackageDir);
    }
  }

  /**
   * Gets the destination file to copy the contents of a media package element.
   *
   * @return The file to copy the content to
   */
  private File getDistributionFile(String channelId, MediaPackage mediapackage, MediaPackageElement element) {

    final String orgId = securityService.getOrganization().getId();
    final Path distributionPath = distributionDirectory.toPath().resolve(orgId);
    final URI elementUri = element.getURI();
    URI relativeUri;

    if (adaptiveStreamingUri != null) {
      relativeUri = adaptiveStreamingUri.relativize(elementUri);
      if (relativeUri != elementUri) {
        // SMIL file

        // Get the relative URL path
        String uriPath = relativeUri.getPath();
        // Remove the last part (corresponds to the part of the "virtual" manifests)
        uriPath = uriPath.substring(0, uriPath.lastIndexOf('/'));
        // Remove the "smil:" tags, if any, and set the right extension if needed
        uriPath = uriPath.replace("smil:", "");
        if (!uriPath.endsWith(".smil"))
          uriPath += ".smil";

        String[] uriPathParts = uriPath.split("/");

        if (uriPathParts.length > 1) {
          logger.warn(
                  "Malformed URI path \"{}\". The SMIL files must be at the streaming application's root. Trying anyway...",
                  uriPath);
        }
        return distributionPath.resolve(uriPath).toFile();
      }
    }

    if (streamingUri != null) {
      relativeUri = streamingUri.relativize(elementUri);
      if (relativeUri != elementUri) {
        // RTMP file

        // Get the relativized URL path
        String urlPath = relativeUri.getPath();
        // Remove the "mp4:" tags, if any, and set the right extension if needed
        urlPath = urlPath.replace("mp4:", "");
        if (!urlPath.endsWith(".mp4"))
          urlPath += ".mp4";

        String[] urlPathParts = urlPath.split("/");

        if (urlPathParts.length < 5) {
          logger.warn(
                  format("Malformed URI %s. Must be of format .../{orgId}/{channelId}/{mediapackageId}/{elementId}/{fileName}."
                          + " Trying URI with current orgId", elementUri));
        }
        return distributionPath.resolve(urlPath).toFile();
      }
    }
    // We have an ordinary file (not yet distributed)
    return new File(getElementDirectory(channelId, mediapackage, element.getIdentifier()),
            FilenameUtils.getName(elementUri.getPath()));
  }

  /**
   * Gets the directory containing the distributed files for this mediapackage.
   *
   * @return the filesystem directory
   */
  private File getMediaPackageDirectory(String channelId, MediaPackage mediaPackage) {
    final String orgId = securityService.getOrganization().getId();
    return distributionDirectory.toPath().resolve(Paths.get(orgId, channelId, mediaPackage.getIdentifier().compact()))
            .toFile();
  }

  /**
   * Gets the directory containing the distributed file for this elementId.
   *
   * @return the filesystem directory
   */
  private File getElementDirectory(String channelId, MediaPackage mediaPackage, String elementId) {
    return new File(getMediaPackageDirectory(channelId, mediaPackage), elementId);
  }

  /**
   * Gets the URI for the element to be distributed.
   *
   * @return The resulting URI after distribution
   * @throws URISyntaxException
   *           if the concrete implementation tries to create a malformed uri
   */
  private URI getDistributionUri(String channelId, MediaPackage mp, MediaPackageElement element)
          throws URISyntaxException {
    String elementId = element.getIdentifier();
    String fileName = FilenameUtils.getBaseName(element.getURI().toString());
    String tag = FilenameUtils.getExtension(element.getURI().toString()) + ":";

    // removes the tag for flv files, but keeps it for all others (mp4 needs it)
    if ("flv:".equals(tag))
      tag = "";

    return UriBuilder.fromUri(streamingUri).path(tag + channelId).path(mp.getIdentifier().compact()).path(elementId)
            .path(fileName).build();
  }

  /**
   * Gets the URI for the element to be distributed.
   *
   * @return The resulting URI after distributionthFromSmil
   */
  private String getAdaptiveDistributionName(String channelId, MediaPackage mp, MediaPackageElement element) {
    String elementId = element.getIdentifier();
    String fileName = FilenameUtils.getBaseName(element.getURI().toString());
    String tag = FilenameUtils.getExtension(element.getURI().toString()) + ":";

    // removes the tag for flv files, but keeps it for all others (mp4 needs it)
    if ("flv:".equals(tag))
      tag = "";
    return tag + channelId + "/" + mp.getIdentifier().compact() + "/" + elementId + "/" + fileName;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    try {
      op = Operation.valueOf(operation);
      String channelId = arguments.get(0);
      MediaPackage mediapackage = MediaPackageParser.getFromXml(arguments.get(1));
      Set<String> elementIds = gson.fromJson(arguments.get(2), new TypeToken<Set<String>>() {
      }.getType());

      List<MediaPackageElement> elements;
      switch (op) {
        case Distribute:
          elements = distributeElements(channelId, mediapackage, elementIds);
          break;
        case Retract:
          elements = retractElements(channelId, mediapackage, elementIds);
          break;
        default:
          throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'");
      }
      if (!elements.isEmpty()) {
        return MediaPackageElementParser.getArrayAsXml(elements);
      }
      return null;
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  private Set<MediaPackageElement> getElements(MediaPackage mediapackage, Set<String> elementIds)
          throws IllegalStateException {
    final Set<MediaPackageElement> elements = new HashSet<>();
    for (String elementId : elementIds) {
       final MediaPackageElement element = mediapackage.getElementById(elementId);
       if (element != null) {
         elements.add(element);
       } else {
         logger.debug("No element " + elementId + " found in media package " + mediapackage.getIdentifier());
       }
    }
    return elements;
  }

}
