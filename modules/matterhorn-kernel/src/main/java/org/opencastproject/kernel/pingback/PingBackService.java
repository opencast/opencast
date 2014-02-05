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
package org.opencastproject.kernel.pingback;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.systems.MatterhornConstans;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Service that will send information on the current installation back to the opencast community. It can easily be
 * enabled and disabled in the main configuration file.
 */
public class PingBackService {

  private static final Logger logger = LoggerFactory.getLogger(PingBackService.class);

  /** Pingback timer */
  private Timer pingbackTimer = null;

  /** The trusted http client used to connect to the runtime info ui */
  private TrustedHttpClient httpClient = null;

  /** Name of the "id" form parameters */
  private static final String PARAM_NAME_ID = "form_id";

  /** Name of the "submitted" form parameters */
  private static final String PARAM_NAME_SUBMITTED = "submitted[data]";

  /** Value of the "id" form parameters */
  private static final String PARAM_VALUE_ID = "webform_client_form_1445";

  /**
   * Osgi callback that is executed on component activation.
   *
   * @param cc
   *          the component context
   */
  public void activate(ComponentContext cc) {
    BundleContext bundleContext = cc.getBundleContext();
    logger.debug("start()");
    // Pingback server, if enabled
    final String pingbackUrl = bundleContext.getProperty("org.opencastproject.anonymous.feedback.url");
    final String hostUrl = bundleContext.getProperty(MatterhornConstans.SERVER_URL_PROPERTY);
    if (StringUtils.isNotBlank(pingbackUrl) && StringUtils.isNotBlank(hostUrl)) {
      try {
        final URI uri = new URI(pingbackUrl);
        pingbackTimer = new Timer("Anonymous Feedback Service", true);
        pingbackTimer.schedule(new TimerTask() {
          public void run() {
            HttpClient httpClient = new DefaultHttpClient();
            try {
              HttpPost post = new HttpPost(uri);
              List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
              // TODO: we are currently using drupal to store this information. Use something less demanding so
              // we can simply post the data.
              params.add(new BasicNameValuePair(PARAM_NAME_ID, PARAM_VALUE_ID));
              params.add(new BasicNameValuePair(PARAM_NAME_SUBMITTED, getRuntimeInfo(hostUrl)));
              UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
              post.setEntity(entity);
              HttpResponse response = httpClient.execute(post);
              logger.debug("Received pingback response: {}", response);
            } catch (Exception e) {
              logger.info("Unable to send system configuration to opencastproject.org: {}", e.getMessage());
            } finally {
              httpClient.getConnectionManager().shutdown();
            }
          }

        }, TimeUnit.MINUTES.toMillis(1), TimeUnit.DAYS.toMillis(7));
        // wait one minute to send first message, and send once a week thereafter
      } catch (URISyntaxException e1) {
        logger.warn("Can not ping back to '{}'", pingbackUrl);
      }
    }
  }

  /**
   * Osgi callback that is executed on component de-activation.
   */
  public void deactivate() {
    if (pingbackTimer != null) {
      pingbackTimer.cancel();
    }
  }

  /**
   * Returns the runtime information as a JSON string.
   *
   * @return the runtime information
   * @throws IOException
   *           if reading the response body fails
   */
  private String getRuntimeInfo(String hostUrl) throws IOException {
    HttpGet get = new HttpGet(UrlSupport.concat(hostUrl, "/info/components.json"));
    HttpResponse response = null;
    try {
      response = httpClient.execute(get);
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
        return null;
      return EntityUtils.toString(response.getEntity());
    } finally {
      httpClient.close(response);
    }
  }

  /**
   * Osgi callback that adds a reference to the trusted http client implementation.
   *
   * @param client
   *          the trusted http client
   */
  void setTrustedHttpClient(TrustedHttpClient client) {
    this.httpClient = client;
  }

}
