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
package org.opencastproject.workflow.handler.mediapackagepost;




import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.json.XML;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;

/**
 * Workflow Operation for POSTing a MediaPackage via HTTP
 */
public class MediaPackagePostOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MediaPackagePostOperationHandler.class);

  /** search service **/
  private SearchService searchService;

  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#getConfigurationOptions()
   */
  @Override
    public SortedMap<String, String> getConfigurationOptions() {
      return Configuration.OPTIONS;
    }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
    throws WorkflowOperationException {

    // get configuration
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();
    Configuration config = new Configuration(currentOperation);

    MediaPackage mp = workflowInstance.getMediaPackage();

    /* Check if we need to replace the Mediapackage we got with the published
     * Mediapackage from the Search Service */
    if (config.mpFromSearch()) {
      SearchQuery searchQuery = new SearchQuery();
      searchQuery.withId(mp.getIdentifier().toString());
      SearchResult result = searchService.getByQuery(searchQuery);
      if (result.size() != 1) {
          throw new WorkflowOperationException("Received multiple results for identifier"
              + "\"" + mp.getIdentifier().toString() + "\" from search service. ");
      }
      logger.info("Getting Mediapackage from Search Service");
      mp = result.getItems()[0].getMediaPackage();
    }

    try {

      logger.info("Submitting \"" + mp.getTitle() + "\" (" + mp.getIdentifier().toString() + ") as "
          + config.getFormat().name() + " to " + config.getUrl().toString());

      // serialize MediaPackage to target format
      OutputStream serOut = new ByteArrayOutputStream();
      MediaPackageParser.getAsXml(mp, serOut, false);
      String mpStr = serOut.toString();
      serOut.close();
      if (config.getFormat() == Configuration.Format.JSON) {
         JSONObject json = XML.toJSONObject(mpStr);
         mpStr = json.toString();
         if (mpStr.startsWith("{\"ns2:")) {
            mpStr = (new StringBuilder()).append("{\"").append(mpStr.substring(6)).toString();
         }
      }

      // Log mediapackge
      if (config.debug()) {
        logger.info(mpStr);
      }

      // constrcut message body
      List<NameValuePair> data = new ArrayList<NameValuePair>();
      data.add(new BasicNameValuePair("mediapackage", mpStr));
      data.addAll(config.getAdditionalFields());

      // construct POST
      HttpPost post = new HttpPost(config.getUrl());
      post.setEntity(new UrlEncodedFormEntity(data, config.getEncoding()));

      // execute POST
      DefaultHttpClient client = new DefaultHttpClient();

      // Handle authentication
      if (config.authenticate()) {
        URL targetUrl = config.getUrl().toURL();
        client.getCredentialsProvider().setCredentials(
            new AuthScope(targetUrl.getHost(), targetUrl.getPort()), config.getCredentials());
      }

      HttpResponse response = client.execute(post);

      // throw Exception if target host did not return 200
      int status = response.getStatusLine().getStatusCode();
      if (status == 200) {
        if (config.debug()) {
          logger.info("Successfully submitted \"" + mp.getTitle()
              + "\" (" + mp.getIdentifier().toString() + ") to " + config.getUrl().toString()
              + ": 200 OK");
        }
      } else if (status == 418) {
        logger.warn("Submitted \"" + mp.getTitle() + "\" (" 
            + mp.getIdentifier().toString() + ") to " + config.getUrl().toString()
            + ": The target claims to be a teapot. "
            + "The Reason for this is probably an insane programmer.");
      } else {
        throw new WorkflowOperationException("Faild to submit \"" + mp.getTitle()
            + "\" (" + mp.getIdentifier().toString() + "), " + config.getUrl().toString()
            + " answered with: " + Integer.toString(status));
      }
    } catch (Exception e) {
      if (e instanceof WorkflowOperationException) {
        throw (WorkflowOperationException) e;
      } else {
        throw new WorkflowOperationException(e);
      }
    }
    return createResult(mp, Action.CONTINUE);
  }

  // <editor-fold defaultstate="collapsed" desc="Inner class that wraps around this WorkflowOperations Configuration">
  private static class Configuration {

    public static enum Format {
      XML, JSON
    };

    // Key for the WorkflowOperation Configuration
    public static final String PROPERTY_URL = "url";
    public static final String PROPERTY_FORMAT = "format";
    public static final String PROPERTY_ENCODING = "encoding";
    public static final String PROPERTY_AUTH = "auth.enabled";
    public static final String PROPERTY_AUTHUSER = "auth.username";
    public static final String PROPERTY_AUTHPASSWD = "auth.password";
    public static final String PROPERTY_DEBUG = "debug";
    public static final String PROPERTY_MEDIAPACKAGE_TYPE = "mediapackage.type";
    public static final TreeMap<String, String> OPTIONS;

    static {
      OPTIONS = new TreeMap<String, String>();
      OPTIONS.put(Configuration.PROPERTY_URL, 
          "The URL to which the MediaPackage will be submitted");
      OPTIONS.put(Configuration.PROPERTY_FORMAT, 
          "The output format for the MediaPackage (default: XML)");
      OPTIONS.put(Configuration.PROPERTY_ENCODING, 
          "Message Encoding (default: UTF-8)");
      OPTIONS.put(Configuration.PROPERTY_AUTH, 
          "The authentication method to use (no/http-digest)");
      OPTIONS.put(Configuration.PROPERTY_AUTHUSER, 
          "The username to use for authentication");
      OPTIONS.put(Configuration.PROPERTY_AUTHPASSWD, 
          "The password to use for authentication");
      OPTIONS.put(Configuration.PROPERTY_DEBUG, 
          "If this options is set the message body returned by target host is dumped to log (default: no)");
      OPTIONS.put(Configuration.PROPERTY_MEDIAPACKAGE_TYPE, 
          "Type of Mediapackage to send (workflow, search; default: search)");
    }

    // Configuration values
    private URI url;
    private Format format = Format.XML;
    private String encoding = "UTF-8";
    private boolean authenticate = false;
    private UsernamePasswordCredentials credentials = null;
    private List<NameValuePair> additionalFields = new ArrayList<NameValuePair>();
    private boolean debug = false;
    private boolean mpFromSearch = true;

    public Configuration(WorkflowOperationInstance operation) throws WorkflowOperationException {
      try {
        Set<String> keys = operation.getConfigurationKeys();

        // configure URL
        if (keys.contains(PROPERTY_URL)) {
          url = new URI(operation.getConfiguration(PROPERTY_URL));
        } else {
          throw new IllegalArgumentException("No target URL provided.");
        }

        // configure format
        if (keys.contains(PROPERTY_FORMAT)) {
          format = Format.valueOf(operation.getConfiguration(PROPERTY_FORMAT).toUpperCase());
        }

        // configure message encoding
        if (keys.contains(PROPERTY_ENCODING)) {
          encoding = operation.getConfiguration(PROPERTY_ENCODING);
        }

        // configure authentication
        if (keys.contains(PROPERTY_AUTH)) {
          String auth = operation.getConfiguration(PROPERTY_AUTH).toUpperCase();
          if (!("NO").equals(auth) && !("FALSE").equals(auth)) {
            String username = operation.getConfiguration(PROPERTY_AUTHUSER);
            String password = operation.getConfiguration(PROPERTY_AUTHPASSWD);
            if (username == null || password == null) {
              throw new WorkflowOperationException("Username and Password must be provided for authentication!");
            }
            credentials = new UsernamePasswordCredentials(username, password);
            authenticate = true;
          }
        }

        // Configure debug mode
        if (keys.contains(PROPERTY_DEBUG))
        {
          String debugstr = operation.getConfiguration(PROPERTY_DEBUG).trim().toUpperCase();
          debug = "YES".equals(debugstr) || "TRUE".equals(debugstr);
        }

        // Configure debug mode
        if (keys.contains(PROPERTY_MEDIAPACKAGE_TYPE))
        {
          String cfgval = operation.getConfiguration(PROPERTY_MEDIAPACKAGE_TYPE).trim().toUpperCase();
          mpFromSearch = "SEARCH".equals(cfgval);
        }

        // get additional form fields
        for (Iterator<String> iter = operation.getConfigurationKeys().iterator(); iter.hasNext();) {
          String key = iter.next();
          if (key.startsWith("+")) {
            String value = operation.getConfiguration(key);
            additionalFields.add(new BasicNameValuePair(key.substring(1), value));
          }
        }
      } catch (Exception e) {
        throw new WorkflowOperationException("Faild to configure operation instance.", e);
      }
    }

    public URI getUrl() {
      return url;
    }

    public Format getFormat() {
      return format;
    }

    public String getEncoding() {
      return encoding;
    }

    public boolean authenticate() {
      return authenticate;
    }

    public UsernamePasswordCredentials getCredentials() {
      return credentials;
    }

    public List<NameValuePair> getAdditionalFields() {
      return additionalFields;
    }

    public boolean debug() {
      return debug;
    }

    public boolean mpFromSearch() {
      return mpFromSearch;
    }
  }

  // </editor-fold>
}
