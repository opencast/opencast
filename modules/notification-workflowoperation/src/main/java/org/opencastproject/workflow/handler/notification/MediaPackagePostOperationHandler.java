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

package org.opencastproject.workflow.handler.notification;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Workflow Operation for POSTing a MediaPackage via HTTP
 */
@Component(
    immediate = true,
    service = WorkflowOperationHandler.class,
    property = {
        "service.description=Workflow Operation that POSTs MediaPackages via HTTP",
        "workflow.operation=post-mediapackage"
    }
)
public class MediaPackagePostOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MediaPackagePostOperationHandler.class);

  /** search service **/
  private SearchService searchService;

  @Reference
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  @Reference
  @Override
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    super.setServiceRegistry(serviceRegistry);
  }

  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {

    // get configuration
    WorkflowOperationInstance currentOperation = workflowInstance.getCurrentOperation();
    Configuration config = new Configuration(currentOperation);

    MediaPackage workflowMP = workflowInstance.getMediaPackage();
    MediaPackage mp = workflowMP;

    // check if we need to replace the media package we got with the published
    // media package from the search service
    if (config.mpFromSearch()) {
      SearchQuery searchQuery = new SearchQuery();
      searchQuery.withId(mp.getIdentifier().toString());
      SearchResult result = searchService.getByQuery(searchQuery);
      if (result.size() != 1) {
        throw new WorkflowOperationException("Received multiple results for identifier"
            + "\"" + mp.getIdentifier().toString() + "\" from search service. ");
      }
      logger.info("Getting media package from search service");
      mp = result.getItems()[0].getMediaPackage();
    }

    logger.info("Submitting {} ({}) as {} to {}",
        mp.getTitle(), mp.getIdentifier(), config.getFormat().name(), config.getUrl());

    try {
      // serialize MediaPackage to target format
      String mpStr;
      if (config.getFormat() == Configuration.Format.JSON) {
        mpStr = MediaPackageParser.getAsJSON(mp);
      } else {
        mpStr = MediaPackageParser.getAsXml(mp);
      }

      // Log media packge
      if (config.debug()) {
        logger.info(mpStr);
      }

      // construct message body
      List<NameValuePair> data = new ArrayList<>();
      data.add(new BasicNameValuePair("mediapackage", mpStr));
      data.addAll(config.getAdditionalFields());

      // construct POST
      HttpPost post = new HttpPost(config.getUrl());
      post.setEntity(new UrlEncodedFormEntity(data, config.getEncoding()));

      // execute POST
      HttpClientBuilder clientBuilder = HttpClientBuilder.create();

      // Handle authentication
      if (config.authenticate()) {
        URL targetUrl = config.getUrl().toURL();
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(
            new AuthScope(targetUrl.getHost(), targetUrl.getPort()),
            config.getCredentials());
        clientBuilder.setDefaultCredentialsProvider(provider);
      }
      CloseableHttpClient client = clientBuilder.build();

      HttpResponse response = client.execute(post);

      // throw Exception if target host did not return 200
      int status = response.getStatusLine().getStatusCode();
      if ((status >= 200) && (status < 300)) {
        if (config.debug()) {
          logger.info("Successfully submitted '{}' ({}) to {}: {}", mp.getTitle(), mp.getIdentifier(),
              config.getUrl(), status);
        }
      } else if (status == 418) {
        logger.warn("Submitted '{}' ({}) to {}: The target claims to be a teapot. "
                + "The Reason for this is probably an insane developer. Go and help that person!",
            mp.getTitle(), mp.getIdentifier(), config.getUrl());
      } else {
        throw new WorkflowOperationException("Failed to submit \"" + mp.getTitle()
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
    return createResult(workflowMP, Action.CONTINUE);
  }

  // <editor-fold defaultstate="collapsed" desc="Inner class that wraps around this WorkflowOperations Configuration">
  private static class Configuration {

    public enum Format {
      XML, JSON
    }

    // Key for the WorkflowOperation Configuration
    public static final String PROPERTY_URL = "url";
    public static final String PROPERTY_FORMAT = "format";
    public static final String PROPERTY_ENCODING = "encoding";
    public static final String PROPERTY_AUTH = "auth.enabled";
    public static final String PROPERTY_AUTHUSER = "auth.username";
    public static final String PROPERTY_AUTHPASSWD = "auth.password";
    public static final String PROPERTY_DEBUG = "debug";
    public static final String PROPERTY_MEDIAPACKAGE_TYPE = "mediapackage.type";

    // Configuration values
    private URI url;
    private Format format = Format.XML;
    private String encoding = "UTF-8";
    private boolean authenticate = false;
    private UsernamePasswordCredentials credentials = null;
    private List<NameValuePair> additionalFields = new ArrayList<NameValuePair>();
    private boolean debug = false;
    private boolean mpFromSearch = true;

    Configuration(WorkflowOperationInstance operation) throws WorkflowOperationException {
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
        if (keys.contains(PROPERTY_DEBUG)) {
          String debugstr = operation.getConfiguration(PROPERTY_DEBUG).trim().toUpperCase();
          debug = "YES".equals(debugstr) || "TRUE".equals(debugstr);
        }

        // Configure debug mode
        if (keys.contains(PROPERTY_MEDIAPACKAGE_TYPE)) {
          String cfgval = operation.getConfiguration(PROPERTY_MEDIAPACKAGE_TYPE).trim().toUpperCase();
          mpFromSearch = "SEARCH".equals(cfgval);
        }

        // get additional form fields
        for (String key : operation.getConfigurationKeys()) {
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
