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
package org.opencastproject.distribution.download.remote;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.serviceregistry.api.RemoteBase;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * A remote distribution service invoker.
 */
public class DownloadDistributionServiceRemoteImpl extends RemoteBase implements DistributionService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(DownloadDistributionServiceRemoteImpl.class);

  /** The property to look up and append to REMOTE_SERVICE_TYPE_PREFIX */
  public static final String REMOTE_SERVICE_CHANNEL = "distribution.channel";

  /** The distribution channel identifier */
  protected String distributionChannel;

  public DownloadDistributionServiceRemoteImpl() {
    super(JOB_TYPE_PREFIX + ".download");
  }

  /** activates the component */
  protected void activate(ComponentContext cc) {
    this.distributionChannel = (String) cc.getProperties().get(REMOTE_SERVICE_CHANNEL);
    super.serviceType = JOB_TYPE_PREFIX + this.distributionChannel;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#distribute(org.opencastproject.mediapackage.MediaPackage,
   *      java.lang.String)
   */
  public Job distribute(MediaPackage mediaPackage, String elementId) throws DistributionException {
    String mediapackageXml = MediaPackageParser.getAsXml(mediaPackage);
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediapackage", mediapackageXml));
    params.add(new BasicNameValuePair("elementId", elementId));
    HttpPost post = new HttpPost();
    HttpResponse response = null;
    Job receipt = null;
    try {
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
      post.setEntity(entity);
      response = getResponse(post);
      if (response != null) {
        logger.info("Distributing {} to {}", elementId, distributionChannel);
        try {
          receipt = JobParser.parseJob(response.getEntity().getContent());
          return receipt;
        } catch (Exception e) {
          throw new DistributionException("Unable to distribute mediapackage '" + elementId
                  + "' using a remote distribution service", e);
        }
      }
    } catch (Exception e) {
      throw new DistributionException("Unable to distribute mediapackage " + elementId
              + " using a remote distribution service proxy.", e);
    } finally {
      closeConnection(response);
    }
    throw new DistributionException("Unable to distribute mediapackage " + elementId
            + " using a remote distribution service proxy.");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.distribution.api.DistributionService#retract(MediaPackage, String)
   */
  @Override
  public Job retract(MediaPackage mediaPackage, String elementId) throws DistributionException {
    String mediapackageXml = MediaPackageParser.getAsXml(mediaPackage);
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediapackage", mediapackageXml));
    params.add(new BasicNameValuePair("elementId", elementId));
    HttpPost post = new HttpPost("/retract");
    HttpResponse response = null;
    UrlEncodedFormEntity entity = null;
    try {
      entity = new UrlEncodedFormEntity(params);
    } catch (UnsupportedEncodingException e) {
      throw new DistributionException("Unable to retract mediapackage " + mediaPackage + " for http post", e);
    }
    post.setEntity(entity);
    try {
      response = getResponse(post);
      Job receipt = null;
      if (response != null) {
        logger.info("retracted {} from {}", mediaPackage, distributionChannel);
        try {
          receipt = JobParser.parseJob(response.getEntity().getContent());
          return receipt;
        } catch (Exception e) {
          throw new DistributionException("Unable to retract mediapackage '" + mediaPackage
                  + "' using a remote distribution service", e);
        }
      }
    } finally {
      closeConnection(response);
    }
    throw new DistributionException("Unable to retract mediapackage " + mediaPackage
            + " using a remote distribution service proxy");
  }

}
