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
package org.opencastproject.distribution.streaming.wowza;

import static java.lang.String.format;
import static org.opencastproject.util.PathSupport.path;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.opencastproject.mediapackage.track.TrackImpl;
import org.opencastproject.util.MimeType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Distributes media to the local media delivery directory.
 */
public class StreamingDistributionService extends AbstractJobProducer implements DistributionService {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(StreamingDistributionService.class);

  /** Receipt type */
  public static final String JOB_TYPE = "org.opencastproject.distribution.streaming";

  /** List of available operations on jobs */
  private enum Operation {
    Distribute, Retract
  };

  /** Default distribution directory */
  public static final String DEFAULT_DISTRIBUTION_DIR = "opencast" + File.separator;

  /** The workspace reference */
  protected Workspace workspace = null;

  /** The service registry */
  protected ServiceRegistry serviceRegistry = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** The distribution directory */
  protected File distributionDirectory = null;

  /** The base URL for streaming */
  protected String streamingUrl = null;

  /** The base URL for adaptive streaming */
  protected String adaptiveStreamingUrl = null;

  /**
   * Creates a new instance of the streaming distribution service.
   */
  public StreamingDistributionService() {
    super(JOB_TYPE);
  }

  protected void activate(ComponentContext cc) {
    // Get the configured streaming and server URLs
    if (cc != null) {
      streamingUrl = StringUtils.trimToNull(cc.getBundleContext().getProperty("org.opencastproject.streaming.url"));
      if (streamingUrl == null)
        logger.warn("Stream url was not set (org.opencastproject.streaming.url)");
      else {
        try {
          URI sUri = new URI(streamingUrl);
          adaptiveStreamingUrl = "http://" + sUri.getHost() + ":1935" + sUri.getPath();
        } catch (URISyntaxException ex) {
          logger.warn("Streaming URL {} could not be parsed", streamingUrl);
        }
        logger.info("streaming url is {} and adaptive streaming url is {}", streamingUrl, adaptiveStreamingUrl);
      }

      String distributionDirectoryPath = StringUtils.trimToNull(cc.getBundleContext().getProperty(
              "org.opencastproject.streaming.directory"));
      if (distributionDirectoryPath == null)
        logger.warn("Streaming distribution directory must be set (org.opencastproject.streaming.directory)");
      else {
        distributionDirectory = new File(distributionDirectoryPath);
        if (!distributionDirectory.isDirectory()) {
          try {
            FileUtils.forceMkdir(distributionDirectory);
          } catch (IOException e) {
            throw new IllegalStateException("Distribution directory does not exist and can't be created", e);
          }
        }
      }

      logger.info("Streaming distribution directory is {}", distributionDirectory);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.distribution.api.DistributionService#distribute(String,
   *      org.opencastproject.mediapackage.MediaPackage, String)
   */
  @Override
  public Job distribute(String channelId, MediaPackage mediapackage, String elementId) throws DistributionException,
          MediaPackageException {
    if (mediapackage == null)
      throw new MediaPackageException("Mediapackage must be specified");
    if (elementId == null)
      throw new MediaPackageException("Element ID must be specified");
    if (channelId == null)
      throw new MediaPackageException("Channel ID must be specified");

    if (StringUtils.isBlank(streamingUrl))
      throw new IllegalStateException("Stream url must be set (org.opencastproject.streaming.url)");
    if (distributionDirectory == null)
      throw new IllegalStateException(
              "Streaming distribution directory must be set (org.opencastproject.streaming.directory)");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Distribute.toString(),
              Arrays.asList(channelId, MediaPackageParser.getAsXml(mediapackage), elementId));
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  /**
   * Distribute a Mediapackage element to the download distribution service.
   *
   * @param mediapackage
   *          The media package that contains the element to distribute.
   * @param elementId
   *          The id of the element that should be distributed contained within the media package.
   * @return A reference to the MediaPackageElement that has been distributed.
   * @throws DistributionException
   *           Thrown if the parent directory of the MediaPackageElement cannot be created, if the MediaPackageElement
   *           cannot be copied or another unexpected exception occurs.
   */
  public MediaPackageElement[] distributeElement(String channelId, final MediaPackage mediapackage, String elementId)
          throws DistributionException {
    if (mediapackage == null)
      throw new IllegalArgumentException("Mediapackage must be specified");
    if (elementId == null)
      throw new IllegalArgumentException("Element ID must be specified");
    if (channelId == null)
      throw new IllegalArgumentException("Channel ID must be specified");

    final MediaPackageElement element = mediapackage.getElementById(elementId);

    // Make sure the element exists
    if (element == null)
      throw new IllegalStateException("No element " + elementId + " found in mediapackage");


    // Streaming servers only deal with tracks
    if (!MediaPackageElement.Type.Track.equals(element.getElementType())) {
      logger.debug("Skipping {} {} for distribution to the streaming server", element.getElementType().toString()
              .toLowerCase(), element.getIdentifier());
      return null;
    }

    try {
      File source;
      try {
        source = workspace.get(element.getURI());
      } catch (NotFoundException e) {
        throw new DistributionException("Unable to find " + element.getURI() + " in the workspace", e);
      } catch (IOException e) {
        throw new DistributionException("Error loading " + element.getURI() + " from the workspace", e);
      }
      File destination = getDistributionFile(channelId, mediapackage, element);

      // Put the file in place
      try {
        FileUtils.forceMkdir(destination.getParentFile());
      } catch (IOException e) {
        throw new DistributionException("Unable to create " + destination.getParentFile(), e);
      }
      logger.info("Distributing {} to {}", elementId, destination);

      try {
        FileSupport.link(source, destination, true);
      } catch (IOException e) {
        throw new DistributionException("Unable to copy " + source + " to " + destination, e);
      }

      // Create a representation of the distributed file in the mediapackage
      final MediaPackageElement distributedElement = (MediaPackageElement) element.clone();
      ArrayList<MediaPackageElement> distribution = new ArrayList<MediaPackageElement>();
      try {
        distributedElement.setURI(getDistributionUri(channelId, mediapackage, element));
      } catch (URISyntaxException e) {
        throw new DistributionException("Distributed element produces an invalid URI", e);
      }
      distributedElement.setIdentifier(null);
      setTransport(distributedElement, TrackImpl.StreamingProtocol.RTMP);

      distribution.add(distributedElement);

      String smilFilename = getSmilFilename(distributedElement, mediapackage, channelId);

      if (isAdaptiveStreamingFormat(distributedElement)) {

      // Only if the Smil file does not exist we need to distribute adaptive streams
      // Otherwise the adaptive streams only were extended with new qualities
        boolean smilExists = smilFileExists(smilFilename);
        Document smilXml = getSmilDocument(smilFilename);

        addElementToSmil(smilXml, distributedElement);

        saveSmilFile(smilFilename, smilXml);
        // Only add these URLs if the smil file is just created
        if (! smilExists) {
          distribution.add(createTrackforStreamingProtocol(distributedElement, smilFilename, TrackImpl.StreamingProtocol.HLS));
          distribution.add(createTrackforStreamingProtocol(distributedElement, smilFilename, TrackImpl.StreamingProtocol.HDS));
          distribution.add(createTrackforStreamingProtocol(distributedElement, smilFilename, TrackImpl.StreamingProtocol.SMOOTH));
          distribution.add(createTrackforStreamingProtocol(distributedElement, smilFilename, TrackImpl.StreamingProtocol.DASH));
        }
      }

      logger.info("Distributed file {} to Wowza Server", element);
      return distribution.toArray(new MediaPackageElement[0]);

    } catch (Exception e) {
      logger.warn("Error distributing " + element, e);
      if (e instanceof DistributionException) {
        throw (DistributionException) e;
      } else {
        throw new DistributionException(e);
      }
    }
  }

  private void setTransport(MediaPackageElement element, TrackImpl.StreamingProtocol protocol) {
    if (element instanceof TrackImpl) {
      ((TrackImpl) element).setTransport(protocol);
    }
  }

  private String getSmilFilename(MediaPackageElement distributedElement, MediaPackage mediapackage, String channelId) {
    return channelId + "_" + mediapackage.getIdentifier() + "_" + distributedElement.getFlavor().getType() + ".smil";
  }

  private URI getSmilUri(String smilFilename)throws URISyntaxException  {
    return new URI(UrlSupport.concat(adaptiveStreamingUrl,"smil:" + smilFilename));
  }

  private URI getHlsUri(URI smilURI) throws URISyntaxException {
    return new URI(UrlSupport.concat(smilURI.toString(),"playlist.m3u8"));
  }

  private URI getHdsUri(URI smilURI) throws URISyntaxException {
    return new URI(UrlSupport.concat(smilURI.toString(),"manifest.f4m"));
  }

  private URI getSmoothStreamingUri(URI smilURI) throws URISyntaxException {
    return new URI(UrlSupport.concat(smilURI.toString(),"Manifest"));
  }

  private URI getDashUri(URI smilURI) throws URISyntaxException {
    return new URI(UrlSupport.concat(smilURI.toString(),"manifest_mpm4sav_mvlist.mpd"));
  }

  private boolean smilFileExists(String name) {
    final String directoryName = distributionDirectory.getAbsolutePath();
    File smil = new File(path(directoryName, name));
    return smil.exists();

  }

  private boolean isAdaptiveStreamingFormat(MediaPackageElement element) {
    return element.getURI().toString().contains("mp4:");
  }

  private Document getSmilDocument(String smilFilename) throws DistributionException {

    if (!smilFileExists(smilFilename)) {
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
        logger.error("Could not create XML file for {}.", smilFilename);
        throw new DistributionException("Could not create XML file for " + smilFilename);
      }
    }

    try {
      final String directoryName = distributionDirectory.getAbsolutePath();
      File smil = new File(path(directoryName, smilFilename));
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(smil);

      if (!doc.getDocumentElement().getNodeName().equalsIgnoreCase("smil")) {
        logger.error("XML-File % is not a SMIL file.", smilFilename);
        throw new DistributionException("XML-File " + smilFilename + " is not an SMIL file.");
      }

      return doc;
    } catch (IOException e) {
      logger.error("Could not open SMIL file {}", smilFilename);
      throw new DistributionException("Could not open SMIL file " + smilFilename);
    } catch (ParserConfigurationException e) {
      logger.error("Could not parse SMIL file {}", smilFilename);
      throw new DistributionException("Could not parse SMIL file " + smilFilename);
    } catch (SAXException e) {
      logger.error("Could no valid XML file {}", smilFilename);
      throw new DistributionException("Could no valid XML file " + smilFilename);
    }
  }

  private void saveSmilFile(String smilFilename, Document doc) throws DistributionException {
    try {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult stream = new StreamResult(new File(path(distributionDirectory.getAbsolutePath(),smilFilename)));
      transformer.transform(source, stream);
      logger.info("SMIL file for Wowza server saved at {}", path(distributionDirectory.getAbsolutePath(),smilFilename));
    } catch (TransformerConfigurationException ex) {
      logger.error("Could not write SMIL file {} for distribution", smilFilename);
      throw new DistributionException("Could not write SMIL file " + smilFilename + " for distribution");
    } catch (TransformerException ex) {
      logger.error("Could not write SMIL file {} for distribution", smilFilename);
      throw new DistributionException("Could not write SMIL file " + smilFilename + " for distribution");
    }
  }

  private String getStreamingPathForSmil(TrackImpl track) {
    String path = track.getURI().getPath();
    return path.substring(path.indexOf("/", 2) + 1);
  }

  private void addElementToSmil(Document doc, MediaPackageElement element) {
    if (! (element instanceof TrackImpl)) return;
    TrackImpl track = (TrackImpl) element;
    NodeList switchElementsList = doc.getElementsByTagName("switch");
    Node switchElement = null;

    //TODO filter for MP4 files only!!

    // There should only be one switch element in of file. If there are more we will igore this.
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

    video.setAttribute("src", getStreamingPathForSmil(track));

    float bitrate = 0;
    for (int i = 0; i < track.getAudio().size(); i++) {
      bitrate += track.getAudio().get(i).getBitRate();
    }
    for (int i = 0; i < track.getVideo().size(); i++) {
      bitrate += track.getVideo().get(i).getBitRate();
    }
    video.setAttribute("system-bitrate", new Integer((int) bitrate).toString());

    switchElement.appendChild(video);
  }

  private void removeElementFromSmil(Document doc, MediaPackageElement element) {
    if (! (element instanceof TrackImpl)) {
      return;
    }
    TrackImpl track = (TrackImpl) element;
    NodeList videoList = doc.getElementsByTagName("video");
    String path = getStreamingPathForSmil(track);
    for (int i = 0; i < videoList.getLength(); i++) {
      if (videoList.item(i) instanceof Element) {
        if (((Element)videoList.item(i)).getAttribute("src").equalsIgnoreCase(path)) {
          videoList.item(i).getParentNode().removeChild(videoList.item(i));
          return;
        }
      }
    }
  }

  private TrackImpl createTrackforStreamingProtocol(MediaPackageElement distributedElement, String smilFilename, TrackImpl.StreamingProtocol protocol) throws URISyntaxException {
    TrackImpl track = (TrackImpl) distributedElement.clone();

    switch (protocol) {
      case HLS :
        track.setURI(getHlsUri(getSmilUri(smilFilename)));
        track.setMimeType(MimeType.mimeType("application","x-mpegURL"));
        break;
      case HDS :
        track.setURI(getHdsUri(getSmilUri(smilFilename)));
        track.setMimeType(MimeType.mimeType("application","f4m+xml"));
        break;
      case SMOOTH :
        track.setURI(getSmoothStreamingUri(getSmilUri(smilFilename)));
        track.setMimeType(MimeType.mimeType("application","vnd.ms-sstr+xml"));
        break;
      case DASH :
        track.setURI(getDashUri(getSmilUri(smilFilename)));
        track.setMimeType(MimeType.mimeType("application","dash+xml"));
        break;
      default: return null;
    }

    track.setIdentifier(null);
    setTransport(track, protocol);
    track.setAudio(null);
    track.setVideo(null);
    track.setChecksum(null);

    return track;
  }

  private boolean emptySmilDoc(Document doc) {
    return doc.getElementsByTagName("video").getLength() <= 0;
  }

  private void deleteSmilFile(String smilFilename) {
    final String directoryName = distributionDirectory.getAbsolutePath();
    File smil = new File(path(directoryName, smilFilename));
    if (! FileSupport.delete(smil))
      logger.info("Could not delete SMIL file {}", smilFilename);
  }

  /**
   *
   * @param distributedElement
   * @param mediapackage
   * @param channelId
   * @return true if a new SmilFile has been created

  private boolean addTrackToSmilFile (MediaPackageElement distributedElement, MediaPackage mediapackage, String channelId) {
    String smilFilename = getSmilFilename(distributedElement, mediapackage, channelId);
    boolean smilExists = smilFileExists(smilFilename);

    // TODO create method


    return !smilExists;
  }

  /**
   *
   * @param retractedElement
   * @param channelId
   * @return true if the SMIL file was deleted

  private boolean removeTrackFromSmilFile (MediaPackageElement retractedElement, MediaPackage mediapackage, String channelId) {
      String smilFilename = getSmilFilename(retractedElement, mediapackage, channelId);

      // TODO create method

      return ! smilFileExists(smilFilename);
  }  */

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.distribution.api.DistributionService#retract(String,
   *      org.opencastproject.mediapackage.MediaPackage, String) java.lang.String)
   */
  @Override
  public Job retract(String channelId, MediaPackage mediaPackage, String elementId) throws DistributionException {
    if (mediaPackage == null)
      throw new IllegalArgumentException("Mediapackage must be specified");
    if (elementId == null)
      throw new IllegalArgumentException("Element ID must be specified");
    if (channelId == null)
      throw new IllegalArgumentException("Channel ID must be specified");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Retract.toString(),
              Arrays.asList(channelId, MediaPackageParser.getAsXml(mediaPackage), elementId));
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  /**
   * Retracts the mediapackage with the given identifier from the distribution channel.
   *
   * @param channelId
   *          the channel id
   * @param mediapackage
   *          the mediapackage
   * @param elementId
   *          the element identifier
   * @return the retracted element or <code>null</code> if the element was not retracted
   */
  protected MediaPackageElement[] retractElement(String channelId, MediaPackage mediapackage, String elementId)
          throws DistributionException {

logger.info("retraction: starting");

    if (mediapackage == null)
      throw new IllegalArgumentException("Mediapackage must be specified");
    if (elementId == null)
      throw new IllegalArgumentException("Element ID must be specified");

logger.info("retraction: everything accepted");

    // Make sure the element exists
    final MediaPackageElement element = mediapackage.getElementById(elementId);
    if (element == null)
      throw new IllegalStateException("No element " + elementId + " found in mediapackage");

    ArrayList<MediaPackageElement> distribution = new ArrayList<MediaPackageElement>();

    // Has this element been distributed?
    if (element == null || (!(element instanceof TrackImpl)) || (!((TrackImpl)element).getTransport().equals(TrackImpl.StreamingProtocol.RTMP)))
      return null;

    try {
      String smilFilename = getSmilFilename(element, mediapackage, channelId);
      boolean smilFileExists = smilFileExists(smilFilename);
      final File elementFile = getDistributionFile(channelId, mediapackage, element);
      final File mediapackageDir = getMediaPackageDirectory(channelId, mediapackage);

      // Does the file exist? If not, the current element has not been distributed to this channel
      // or has been removed otherwise
      if (!elementFile.exists()) {
        distribution.add(element);
        return distribution.toArray(new MediaPackageElement[0]);
      }

      // Try to remove the file and - if possible - the parent folder
      FileUtils.forceDelete(elementFile);
      File elementDir = elementFile.getParentFile();
      if (elementDir.isDirectory() && elementDir.list().length == 0) {
        FileSupport.delete(elementDir);
      }
      if (mediapackageDir.isDirectory() && mediapackageDir.list().length == 0) {
        FileSupport.delete(mediapackageDir);
      }
      if (smilFileExists) {
        Document smilDocument = getSmilDocument(smilFilename);
        removeElementFromSmil(smilDocument, element);
        saveSmilFile(smilFilename, smilDocument);
        if (emptySmilDoc(smilDocument)) { // only remove adaptive streaming URIs if the smil has been removed
          deleteSmilFile(smilFilename);
          URI smilUri = getSmilUri(smilFilename);
          for (MediaPackageElement e : mediapackage.getElements()) {
            if (getHlsUri(smilUri).equals(e.getURI()) || getHdsUri(smilUri).equals(e.getURI()) || getDashUri(smilUri).equals(e.getURI()) || getSmoothStreamingUri(smilUri).equals(e.getURI())) {
              distribution.add(e);
            }
          }
        }
      }

      logger.info("Finished rectracting element {} of media package {}", elementId, mediapackage);
      distribution.add(element);
      return distribution.toArray(new MediaPackageElement[0]);
    } catch (Exception e) {
      logger.warn("Error retracting element " + elementId + " of mediapackage " + mediapackage, e);
      if (e instanceof DistributionException) {
        throw (DistributionException) e;
      } else {
        throw new DistributionException(e);
      }
    }

  }

  /**
   * Gets the destination file to copy the contents of a mediapackage element.
   *
   * @return The file to copy the content to
   */
  protected File getDistributionFile(String channelId, MediaPackage mp, MediaPackageElement element) {
    String uriString = element.getURI().toString();
    final String directoryName = distributionDirectory.getAbsolutePath();
    if (uriString.startsWith(streamingUrl) || uriString.startsWith(adaptiveStreamingUrl)) {
      if (uriString.lastIndexOf(".") < (uriString.length() - 4)) {
        if (uriString.contains("mp4:")) {
          uriString += ".mp4";
          uriString = uriString.replace("mp4:", "");
        } else uriString += ".flv";
      }
      String url = null;
      if (uriString.startsWith(streamingUrl)) url = streamingUrl;
      else url = adaptiveStreamingUrl;
      String[] splitUrl = uriString.substring(url.length() + 1).split("/");
      if (splitUrl.length < 4) {
        logger.warn(format(
                "Malformed URI %s. Must be of format .../{channelId}/{mediapackageId}/{elementId}/{fileName}."
                        + " Trying URI without channelId", uriString));
        return new File(path(directoryName, splitUrl[0], splitUrl[1], splitUrl[2]));
      } else {
        return new File(path(directoryName, splitUrl[0], splitUrl[1], splitUrl[2], splitUrl[3]));
      }
    }
    return new File(path(directoryName, channelId, mp.getIdentifier().compact(), element.getIdentifier(),
            FilenameUtils.getName(uriString)));
  }

  /**
   * Gets the directory containing the distributed files for this mediapackage.
   *
   * @return the filesystem directory
   */
  protected File getMediaPackageDirectory(String channelId, MediaPackage mediaPackage) {
    return new File(distributionDirectory, path(channelId, mediaPackage.getIdentifier().compact()));
  }

  /**
   * Gets the URI for the element to be distributed.
   *
   * @return The resulting URI after distribution
   * @throws URISyntaxException
   *           if the concrete implementation tries to create a malformed uri
   */
  protected URI getDistributionUri(String channelId, MediaPackage mp, MediaPackageElement element)
          throws URISyntaxException {
    String elementId = element.getIdentifier();
    String fileName = FilenameUtils.getBaseName(element.getURI().toString());
    String tag = FilenameUtils.getExtension(element.getURI().toString()) + ":";

    // removes the tag for flv files, but keeps it for all others (mp4 needs it)
    if ("flv:".equals(tag))
      tag = "";

    return new URI(UrlSupport.concat(streamingUrl, tag + channelId, mp.getIdentifier().compact(), elementId, fileName));
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
      String elementId = arguments.get(2);
      switch (op) {
        case Distribute:
          MediaPackageElement[] distributedElement = distributeElement(channelId, mediapackage, elementId);
          return (distributedElement != null) ? MediaPackageElementParser.getArrayAsXml(Arrays.asList(distributedElement)) : null;
        case Retract:
          MediaPackageElement[] retractedElement = null;
          if (distributionDirectory != null && StringUtils.isNotBlank(streamingUrl)) {
            retractedElement = retractElement(channelId, mediapackage, elementId);
          }
          return (retractedElement != null) ? MediaPackageElementParser.getArrayAsXml(Arrays.asList(retractedElement)) : null;
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  /**
   * Callback for the OSGi environment to set the workspace reference.
   *
   * @param workspace
   *          the workspace
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Callback for the OSGi environment to set the service registry reference.
   *
   * @param serviceRegistry
   *          the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * Callback for setting the security service.
   *
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   *
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   *
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getSecurityService()
   */
  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getUserDirectoryService()
   */
  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#getOrganizationDirectoryService()
   */
  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }
}
