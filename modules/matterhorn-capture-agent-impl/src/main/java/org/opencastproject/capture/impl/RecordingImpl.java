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
package org.opencastproject.capture.impl;

import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.RecordingState;
import org.opencastproject.capture.api.AgentRecording;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageImpl;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.util.ConfigurationException;

import org.apache.commons.io.FileUtils;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

/**
 * This class is a container for the properties relating a certain recording -- a set of Properties and a MediaPackage
 * with all the metadata/attachments/etc. associated
 */
public class RecordingImpl implements AgentRecording, Serializable {

  private static final long serialVersionUID = 1L;

  private static transient Logger logger = LoggerFactory.getLogger(RecordingImpl.class);

  /** Date formatter for the metadata file */
  private static transient SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'");

  /** Unique identifier for this ID */
  protected String id = null;

  /** Directory in the filesystem where the files related with this recording are */
  protected File baseDir = null;

  /** The recording's state. Defined in {@code RecordingState}. */
  protected String state = RecordingState.UNKNOWN;

  /**
   * The time at which the recording last checked in with this service. Note that this is an absolute timestamp (ie,
   * milliseconds since 1970) rather than a relative timestamp (ie, it's been 3000 ms since it last checked in).
   */
  protected Long lastHeardFrom;

  /** Keeps the properties associated with this recording */
  protected XProperties props = null;

  /** The MediaPackage containing all the metadata/attachments/any file related with this recording */
  protected transient MediaPackage mPkg = null;

  /**
   * Constructs a RecordingImpl object using the Properties and MediaPackage provided
   * 
   * @param Xproperties
   *          The {@code XProperties} object associated to this recording
   * @param mp
   *          The {@code MediaPackage} with this recording files
   * @throws IOException
   *           If the base directory could not be fetched
   */
  public RecordingImpl(MediaPackage mp, XProperties properties) throws IOException {
    // Stores the MediaPackage
    this.mPkg = mp;

    if (properties != null) {
      this.props = (XProperties) properties.clone();
    } else {
      logger.warn("Properties parameter was null, this recording will be in a very weird state!");
    }

    // If the mediapackage is null create a new one
    if (mPkg == null) {
      try {
        mPkg = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
      } catch (ConfigurationException e) {
        throw new RuntimeException("ConfigurationException building default mediapackage!", e);
      } catch (MediaPackageException e) {
        throw new RuntimeException("MediaPackageException building default mediapackage!", e);
      }
    }

    determineRootURLandID();
    mPkg.setIdentifier(new IdImpl(id));

    // Setup the root capture dir, also make sure that it exists.
    if (!baseDir.exists()) {
      try {
        FileUtils.forceMkdir(baseDir);
      } catch (IOException e) {
        logger.error("IOException creating required directory {}.", baseDir.toString());
        // setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
        throw e;
      }
      // Should have been created. Let's make sure of that.
      if (!baseDir.exists()) {
        logger.error("Unable to start capture, could not create required directory {}.", baseDir.toString());
        // setRecordingState(recordingID, RecordingState.CAPTURE_ERROR);
        throw new IOException("Unable to create base directory");
      }
    }

    // Write out the metadata file needed by the core if it's not present
    // TODO: make this a constant?
    File metadataFile = new File(baseDir, "episode.xml");
    if (!metadataFile.exists()) {
      FileWriter out = new FileWriter(metadataFile);
      out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
      out.write("<dublincore xmlns=\"http://www.opencastproject.org/xsd/1.0/dublincore/\""
              + " xmlns:dcterms=\"http://purl.org/dc/terms/\""
              + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
      out.write("<dcterms:created xsi:type=\"dcterms:W3CDTF\">" + formatDate(new Date()) + "</dcterms:created>");
      out.write("<dcterms:identifier>" + id + "</dcterms:identifier>");
      out.write("<dcterms:title>" + id + "</dcterms:title>");
      out.write("</dublincore>");
      out.flush();
      out.close();

      mPkg.add(metadataFile.toURI(), MediaPackageElement.Type.Catalog, MediaPackageElements.EPISODE);
    }
  }

  /**
   * Determines the root URL and ID from the recording's properties //TODO: What if the properties object contains a
   * character in the recording id or root url fields that is invalid for the filesystem?
   * 
   * @throws IOException
   */
  private void determineRootURLandID() throws IOException {

    if (props == null) {
      logger.info("Properties are null for recording, guessing that the root capture dir is java.io.tmpdir...");
      props = new XProperties();
      props.setProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL, System.getProperty("java.io.tmpdir"));
    }

    // Figures out where captureDir lives
    if (this.props.containsKey(CaptureParameters.RECORDING_ROOT_URL)) {
      baseDir = new File(props.getProperty(CaptureParameters.RECORDING_ROOT_URL));
      if (props.containsKey(CaptureParameters.RECORDING_ID)) {
        // In this case they've set both the root URL and the recording ID, so we're done.
        id = props.getProperty(CaptureParameters.RECORDING_ID);
      } else {
        // In this case they've set the root URL, but not the recording ID. Get the id from that url instead then.
        logger.debug("{} was set, but not {}.", CaptureParameters.RECORDING_ROOT_URL, CaptureParameters.RECORDING_ID);
        id = new File(props.getProperty(CaptureParameters.RECORDING_ROOT_URL)).getName();
        props.put(CaptureParameters.RECORDING_ID, id);
      }
    } else {
      File cacheDir = new File(props.getProperty(CaptureParameters.CAPTURE_FILESYSTEM_CAPTURE_CACHE_URL));
      // If there is a recording ID use it, otherwise it's unscheduled so just grab a timestamp
      if (props.containsKey(CaptureParameters.RECORDING_ID)) {
        id = props.getProperty(CaptureParameters.RECORDING_ID);
        baseDir = new File(cacheDir, id);
      } else {
        // Unscheduled capture, use a timestamp value instead
        id = "Unscheduled-" + props.getProperty(CaptureParameters.AGENT_NAME) + "-" + System.currentTimeMillis();
        props.setProperty(CaptureParameters.RECORDING_ID, id);
        baseDir = new File(cacheDir, id);
      }
      props.put(CaptureParameters.RECORDING_ROOT_URL, baseDir.getCanonicalPath());
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.AgentRecording#getProperties()
   */
  public XProperties getProperties() {
    return props;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.AgentRecording#setProperties(java.util.Properties)
   */
  public void setProps(XProperties props) {
    this.props = props;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.AgentRecording#setProperties(java.util.Properties)
   */
  public void setProperties(Properties props) {
    // Preserve the bundle context between property lists
    BundleContext ctx = null;
    if (this.props != null) {
      ctx = this.props.getBundleContext();
    }
    this.props = new XProperties();
    this.props.setBundleContext(ctx);

    for (String key : props.stringPropertyNames()) {
      props.put(key, props.getProperty(key));
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.AgentRecording#getMediaPackage()
   */
  public MediaPackage getMediaPackage() {
    return mPkg;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.Recording#getID()
   */
  public String getID() {
    return id;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.AgentRecording#getDir()
   */
  public File getBaseDir() {
    return baseDir;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.AgentRecording#getProperty(java.lang.String)
   */
  public String getProperty(String key) {
    return props.getProperty(key);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.api.AgentRecording#setProperty(java.lang.String, java.lang.String)
   */
  public String setProperty(String key, String value) {
    return (String) props.setProperty(key, value);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.Recording#setState(java.lang.String)
   */
  public void setState(String state) {
    if (state == null) {
      this.state = RecordingState.UNKNOWN;
    } else {
      this.state = state;
    }
    lastHeardFrom = System.currentTimeMillis();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.Recording#getState()
   */
  public String getState() {
    return state;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.capture.admin.api.Recording#getLastCheckinTime()
   */
  public Long getLastCheckinTime() {
    return lastHeardFrom;
  }

  /**
   * Formats a Date object to UTC time and according to the dublin core rules for dcterms:created
   * 
   * @param d
   *          The Date to format
   * @return The formatted Date
   */
  private static synchronized String formatDate(Date d) {
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    return sdf.format(d);
  }

  /**
   * Overrides the default serialization behaviour. This method writes the mediapackage to the mediapackage.xml file in
   * the base directory of this capture
   * 
   * @param out
   *          The ObjectOutputStream for the serialization
   * @throws IOException
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    try {
      MediaPackageParser.getAsXml(mPkg, out, true);
    } catch (MediaPackageException e) {
      logger.error("Unable to write mediapackage to disk!  Error was: {}.", e);
    }
  }

  /**
   * Overrides the default serialization behaviour. This method reads the mediapackage from the mediapackage.xml file in
   * the base directory of this capture
   * 
   * @param in
   *          The ObjectInputStream for the serialization
   * @throws IOException
   * @throws ClassNotFoundException
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    try {
      mPkg = MediaPackageImpl.valueOf(in);
    } catch (MediaPackageException e) {
      logger.error("Unable to read mediapackage from disk!  Error was: {}.", e);
    }
  }
}
