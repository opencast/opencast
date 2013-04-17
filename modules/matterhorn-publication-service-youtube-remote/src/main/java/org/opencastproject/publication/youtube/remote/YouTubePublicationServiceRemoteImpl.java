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
package org.opencastproject.publication.youtube.remote;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.publication.api.YouTubePublicationService;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A remote youtube service invoker.
 */
public class YouTubePublicationServiceRemoteImpl extends RemoteBase implements YouTubePublicationService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(YouTubePublicationServiceRemoteImpl.class);

  public YouTubePublicationServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  @Override
  public Job publish(MediaPackage mediaPackage, Track track) throws PublicationException {
    final String trackId = track.getIdentifier();
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediaPackage)));
    params.add(new BasicNameValuePair("elementId", trackId));
    HttpPost post = new HttpPost();
    HttpResponse response = null;
    try {
      post.setEntity(new UrlEncodedFormEntity(params));
      response = getResponse(post);
      if (response != null) {
        logger.info("Publishing {} to youtube", trackId);
        return JobParser.parseJob(response.getEntity().getContent());
      }
    } catch (Exception e) {
      throw new PublicationException("Unable to publish track " + trackId + " from mediapackage "
              + mediaPackage + " using a remote youtube publication service", e);
    } finally {
      closeConnection(response);
    }
    throw new PublicationException("Unable to publish track " + trackId + " from mediapackage "
            + mediaPackage + " using a remote youtube publication service");
  }

  @Override
  public Job retract(MediaPackage mediaPackage) throws PublicationException {
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediaPackage)));
    HttpPost post = new HttpPost("/retract");
    HttpResponse response = null;
    try {
      post.setEntity(new UrlEncodedFormEntity(params));
      response = getResponse(post);
      if (response != null) {
        logger.info("Retracting {} from youtube", mediaPackage);
        return JobParser.parseJob(response.getEntity().getContent());
      }
    } catch (Exception e) {
      throw new PublicationException("Unable to retract mediapackage " + mediaPackage
              + " using a remote youtube publication service", e);
    } finally {
      closeConnection(response);
    }
    throw new PublicationException("Unable to retract mediapackage " + mediaPackage
            + " using a remote youtube publication service");
  }

}
