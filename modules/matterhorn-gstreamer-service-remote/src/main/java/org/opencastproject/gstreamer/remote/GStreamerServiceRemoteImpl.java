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
package org.opencastproject.gstreamer.remote;

import org.opencastproject.gstreamer.service.api.GStreamerLaunchException;
import org.opencastproject.gstreamer.service.api.GStreamerService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.serviceregistry.api.RemoteBase;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Proxies a set of remote gstreamer services for use as a JVM-local service. Remote services are selected at random.
 */
public class GStreamerServiceRemoteImpl extends RemoteBase implements GStreamerService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(GStreamerServiceRemoteImpl.class);

  public GStreamerServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.gstreamer.api.GStreamerService#launch(org.opencastproject.mediapackage.MediaPackage,
   *      java.lang.String, java.lang.String)
   */
  public Job launch(MediaPackage mediapackage, String launch, String outputFiles) throws GStreamerLaunchException {
    HttpPost post = new HttpPost("/launch");
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediapackage)));
      params.add(new BasicNameValuePair("launch", launch));
      params.add(new BasicNameValuePair("outputFiles", outputFiles));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new GStreamerLaunchException("Unable to assemble a remote composer request for track " + e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        String content = EntityUtils.toString(response.getEntity());
        Job r = JobParser.parseJob(content);
        logger.info("Encoding job {} started on a remote composer", r.getId());
        return r;
      }
    } catch (Exception e) {
      throw new GStreamerLaunchException("Unable to encode track " + " using a remote composer service", e);
    } finally {
      closeConnection(response);
    }
    throw new GStreamerLaunchException("Unable to encode track " + " using a remote composer service");
  }

}