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

package org.opencastproject.composer.impl.episode;

import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;
import org.opencastproject.composer.impl.episode.XmlRpcJob.XmlRpcJobState;
import org.opencastproject.composer.impl.episode.XmlRpcJob.XmlRpcReason;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.UrlSupport;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class used to monitor the various jobs, based on xmlrpc communication with the Telestream Episode encoder engine.
 */
public class XmlRpcEngineController implements Runnable {

  /** Timeout for retry in case of a communication error */
  private static final int COMM_RETRY_TIMEOUT = 30;

  /** The engine to notify */
  private EpisodeEncoderEngine engine = null;

  /** xmlrpc hostname */
  private String xmlrpcHostname = null;

  /** xmlrpc port */
  private int xmlrpcPort = EpisodeEncoderEngine.DEFAULT_XMLRPC_PORT;

  /** True if a warning about failing communication has been issued */
  private boolean commWarningIssued = false;

  /** xmlrpc path on the server */
  private String xmlrpcPath = null;

  /** xmlrpc password on the server */
  // private String xmlrpcPassword = null;

  /** The running flag */
  private boolean keepRunning = true;

  /** The communication client */
  private XmlRpcClient xmlrpcClient = null;

  /** The list of jobs to watch */
  private final List<XmlRpcJob> joblist = new CopyOnWriteArrayList<XmlRpcJob>();

  /** The timeout */
  private long timeout = EpisodeEncoderEngine.DEFAULT_MONITOR_FREQUENCY * 1000L;

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(XmlRpcEngineController.class.getName());

  /**
   * Creates a new monitor for the given engine.
   *
   * @param engine
   *          the compression engine instance
   * @param host
   *          the xmlrpc hostname
   * @param port
   *          the xmlrpc port number
   * @param path
   *          the xmlrpc path
   * @param password
   *          the xmlrpc password
   * @param timeout
   *          the timeout in milliseconds
   */
  XmlRpcEngineController(EpisodeEncoderEngine engine, String host, int port, String path, String password, long timeout) {
    this.engine = engine;
    this.xmlrpcHostname = host;
    this.xmlrpcPort = port;
    this.xmlrpcPath = path;
    // this.xmlrpcPassword = password;
    this.timeout = timeout;
  }

  /**
   * Stops the encoder controller.
   */
  protected void stop() {
    stop("(unkown reason)");
  }

  /**
   * Stopts the encoder controller.
   *
   * @param reason
   *          the reason for stopping
   */
  void stop(String reason) {
    keepRunning = false;

    // Notify listeners
    for (XmlRpcJob job : joblist) {
      engine.fileEncodingFailed(job.getSourceFile(), job.getEncodingProfile(), reason);
    }
    synchronized (joblist) { // FIXME CopyOnWriteArrayList should never be externally synchronized
      joblist.clear();
    }

    // Disconnect from the engine
    disconnect();
  }

  /**
   * Sets the monitor frequency.
   *
   * @param timeout
   *          the timeout
   */
  void setFrequency(long timeout) {
    this.timeout = timeout;
  }

  /**
   * Submits a job to the watchfolder monitor. The monitor will then notify the engine and registered listeners about
   * the appearance of <code>outfile</code>, once it shows up after processing.
   *
   * @param track
   *          the track that is being encoded
   * @param profiles
   *          the encoding profile
   */
  void submitJob(File track, EncodingProfile format) throws EncoderException {
    List<EpisodeSettings> settings = getSettings(track, format);
    if (settings == null || settings.size() == 0)
      throw new EncoderException(engine, "No settings found for profile '" + format.getIdentifier() + "' and track "
              + track);

    // Prepare the metadata
    Map<String, String> metadata = new Hashtable<String, String>();

    // The job priority
    int priority = EpisodeEncoderEngine.PRIORITY_MEDIUM;

    // Submit a job for every setting in the settings group
    for (EpisodeSettings setting : settings) {
      StringBuffer desc = new StringBuffer();
      desc.append(track);
      desc.append(" to ");
      desc.append(format);
      desc.append(" ");
      desc.append(settings);
      Vector<Object> arguments = new Vector<Object>(5);
      arguments.add(track.getAbsolutePath());
      arguments.add(setting.getPath());
      arguments.add(desc.toString());
      arguments.add(priority);
      arguments.add(metadata);
      Object result = execute("engine.submitJob", arguments);
      if (result instanceof Integer) {
        int jobId = ((Integer) result).intValue();
        synchronized (joblist) { // FIXME CopyOnWriteArrayList should never be externally synchronized
          joblist.add(new XmlRpcJob(jobId, track, format, setting));
        }
        logger.trace("Submitted track " + track + " to episode engine with settings " + setting.getName());
      } else {
        throw new EncoderException(engine, "Unexpected reply from episode engine at " + xmlrpcHostname
                + ": expected job identifier, got " + result);
      }
    }
  }

  /**
   * Connects to the engine's xmlrpc server and returns the corresponding client.
   */
  private XmlRpcClient connect() throws ConfigurationException {
    if (xmlrpcClient != null)
      return xmlrpcClient;
    try {
      URL url = new URL(UrlSupport.concat("http://" + xmlrpcHostname + ":" + xmlrpcPort, xmlrpcPath));
      xmlrpcClient = new XmlRpcClient();
      XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
      config.setServerURL(url);
      xmlrpcClient.setConfig(config);
      // TODO: Authentication is not working. Must have the engine set
      // to "anonymous" password.
      // xmlrpc.setBasicAuthentication("anonymous", xmlrpcPassword);
    } catch (MalformedURLException e) {
      throw new ConfigurationException("Error connecting to episode engine: " + e.getMessage());
    }
    return xmlrpcClient;
  }

  /**
   * Reconnects to the engine's xmlrpc server and returns the corresponding client.
   */
  private XmlRpcClient reconnect() throws ConfigurationException {
    xmlrpcClient = null;
    return connect();
  }

  /**
   * Disconnects from the engine's xmlrpc server and sets the xmlrpc client to <code>null</code>.
   */
  private void disconnect() {
    xmlrpcClient = null;
  }

  /**
   * Returns a list of settings for the specified track and profile. The settings are being looked up in episode's
   * settings folder (usually <tt>/Users/Shared/Episode Engine/Settings</tt>) and inside this folder at location
   * <tt>Opencast/&lt;profile&gt;</tt>
   *
   * @param sourceFile
   *          the file to be encoded
   * @param profile
   *          the profile name
   * @return the settings that are available for this combination
   * @throws EncoderException
   *           if loading the settings failed
   */
  @SuppressWarnings("unchecked")
  private List<EpisodeSettings> getSettings(File sourceFile, EncodingProfile profile) throws EncoderException {
    String settingsPath = PathSupport.concat(new String[] { "Opencast", profile.getIdentifier() });

    logger.trace("Looking for episode settings at " + settingsPath);

    // Prepare the call
    Vector<Object> arguments = new Vector<Object>(1);
    arguments.add(settingsPath);

    // Ask for the settings
    List<EpisodeSettings> settings = null;
    Object result = execute("engine.getSettingsInGroupAtPath", arguments);
    if (!(result instanceof Object[]))
      throw new EncoderException(engine, "Episode engine returned unknown result when asked for settings at "
              + settingsPath);
    Object[] settingsGroup = (Object[]) result;
    settings = new ArrayList<EpisodeSettings>(settingsGroup.length);
    for (Object settingsEntry : settingsGroup) {
      Map<String, Object> s = (Map<String, Object>) settingsEntry;
      if (s.size() != 4)
        throw new EncoderException(engine, "Episode engine returned unknown result when asked for settings at "
                + settingsPath);
      EpisodeSettings es = new EpisodeSettings(s);
      settings.add(es);
    }
    return settings;
  }

  /**
   * Returns <code>true</code> if the engine is processing the specified track for the same media format and profile,
   * using at least one setting.
   *
   * TODO do we need this?
   *
   * @param file
   *          the file
   * @param profile
   *          the profile
   * @return <code>true</code> if the track is still processed
   */
  private boolean fileIsProcessed(File file, EncodingProfile profile) {
    List<XmlRpcJob> jobs = getJobs(file, profile);
    if (jobs.size() > 0)
      logger.trace("File " + file + " is still being processed");
    return jobs.size() > 0;
  }

  /**
   * Returns the jobs that are currently in the system processing the given file with the specified profile.
   *
   * @param sourceFile
   *          the file to be encoded
   * @param profile
   *          the profile
   * @return the list of jobs
   */
  private List<XmlRpcJob> getJobs(File sourceFile, EncodingProfile profile) {
    List<XmlRpcJob> jobs = new ArrayList<XmlRpcJob>();
    synchronized (joblist) { // FIXME CopyOnWriteArrayList should never be externally synchronized
      for (XmlRpcJob job : joblist) {
        if (job.getSourceFile().equals(sourceFile) && job.getEncodingProfile().equals(profile))
          jobs.add(job);
      }
    }
    return jobs;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @SuppressWarnings("unchecked")
  public void run() {

    XmlRpcJobState newState = null;

    while (keepRunning) {
      try {
        // Go through all jobs and see if the status has changed
        for (XmlRpcJob job : joblist) {
          Vector<Object> arguments = new Vector<Object>(1);
          arguments.add(job.getIdentifier());
          Object result = execute("engine.getJobForID", arguments);
          if (!(result instanceof Map))
            throw new EncoderException(engine, "Episode engine returned an illegal state value");
          Map<String, Object> response = (Map<String, Object>) result;
          Map<String, Object> status = (Map<String, Object>) response.get("currentStatus");
          if (status == null)
            throw new EncoderException(engine, "Episode enginge does not return a status");
          newState = XmlRpcJobState.parseResult(status);

          File track = job.getSourceFile();
          EncodingProfile encodingProfile = job.getEncodingProfile();
          EpisodeSettings settings = job.getSettings();

          if (!job.getState().equals(newState)) {

            if (newState == null) {
              logger.error("Lost job state for " + job);
              continue;
            }

            else if (newState.equals(XmlRpcJobState.Created)) {
              // TODO: Process state change
              logger.trace("Episode job " + job + " was created");
            }

            else if (newState.equals(XmlRpcJobState.Queued)) {
              // TODO: Process state change
              logger.debug("Enqueued encoding of " + track + " to " + encodingProfile + " " + settings);
            }

            else if (newState.equals(XmlRpcJobState.Running)) {
              // TODO: Process state change
              logger.debug("Started encoding of " + track + " to " + encodingProfile + " " + settings);
            }

            else if (newState.equals(XmlRpcJobState.Finished)) {

              // Remove job and see if track is still processed with
              // another setting from the same settings group
              boolean trackIsProcessed = false;
              synchronized (joblist) {
                joblist.remove(job);
                trackIsProcessed = fileIsProcessed(track, encodingProfile);
              }

              // Tell engine
              logger.debug("Finished encoding of " + track + " to " + encodingProfile + " " + settings);
              if (!trackIsProcessed) {
                engine.fileEncoded(track, encodingProfile);
              }
            }

            else if (newState.equals(XmlRpcJobState.Stopped)) {

              // Remove job and notify observers regardless of
              // other settings that are applied to this track
              List<XmlRpcJob> associatedJobs = null;
              synchronized (joblist) { // FIXME CopyOnWriteArrayList should never be externally synchronized
                joblist.remove(job);
                associatedJobs = getJobs(track, encodingProfile);
                joblist.removeAll(associatedJobs);
              }

              // Tell engine
              logger.warn("Encoding of " + track + " to " + encodingProfile + " " + settings + " was stopped");
              if (associatedJobs.size() > 0)
                logger.warn(associatedJobs.size() + " associated jobs have been canceled");
              engine.fileEncodingFailed(track, encodingProfile, "Canceled");
            }

            else if (newState.equals(XmlRpcJobState.Failed)) {
              XmlRpcReason reason = XmlRpcReason.parseResult(result);

              // Remove job and notify observers regardless of
              // other settings that are applied to this track
              List<XmlRpcJob> associatedJobs = null;
              synchronized (joblist) { // FIXME CopyOnWriteArrayList should never be externally synchronized
                joblist.remove(job);
                associatedJobs = getJobs(track, encodingProfile);
                joblist.removeAll(associatedJobs);
              }

              // Tell engine
              logger.debug("Encoding of " + track + " to " + encodingProfile + " " + settings + " failed: " + reason);
              if (associatedJobs.size() > 0)
                logger.trace(associatedJobs.size() + " associated jobs have been canceled");
              engine.fileEncodingFailed(track, encodingProfile, reason.toString());
            } else {
              logger.error("Episode engine discovered job with unkown state '" + newState + "'");
            }

            // Remember the new state
            job.setState(newState);
          }

          // Monitor progress of running jobs
          else if (job.getState().equals(XmlRpcJobState.Running)) {
            int progress = ((Integer) status.get("progress")).intValue();
            if (progress - job.getProgress() >= 10) {
              job.setProgress((progress / 10) * 10);
              engine.fileEncodingProgressed(track, encodingProfile, job.getProgress());
              logger.trace("Encoding of " + track + " to " + encodingProfile + " progressed to " + job.getProgress()
                      + "%");
            }
          }

        }
      } catch (Throwable t) {
        // TODO: Think about what to do here.
        logger.error("Episode encoder monitor encountered an exception: " + t.getMessage(), t);
      }

      // Sleep for a few seconds
      try {
        Thread.sleep(timeout);
      } catch (InterruptedException e) {
        // TODO: Think about what to do here.
        logger.trace("Episode encoder monitor interrupted");
      }

    }
  }

  /**
   * Encapsulates communication with episode engine. Since the connection can sometimes be instable, a bit of retry
   * logic has been added here.
   *
   * @param command
   *          the command to execute
   * @param arguments
   *          arguments to the command
   * @return the response or <code>null</code> if the request failed
   */
  private synchronized Object execute(String command, Vector<Object> arguments) throws EncoderException {
    XmlRpcClient client = connect();
    while (keepRunning) {
      String msg = null;
      try {
        Object result = client.execute(command, arguments);
        commWarningIssued = false;
        return result;
      } catch (XmlRpcException e) {
        msg = "Communication error with episode engine: " + e.getMessage();
      }

      // Log this incident
      if (!commWarningIssued) {
        logger.warn(msg);
        commWarningIssued = true;
      }

      // Take a break
      try {
        Thread.sleep(COMM_RETRY_TIMEOUT * 1000L);
      } catch (InterruptedException e) {
      }

      // Get a new connection
      reconnect();
    }

    // The controller is about to be shut down
    throw new EncoderException(engine, "Error communicating with episode engine at " + xmlrpcHostname);
  }

}
