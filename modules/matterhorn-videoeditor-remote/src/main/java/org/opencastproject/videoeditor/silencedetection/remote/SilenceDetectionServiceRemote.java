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
package org.opencastproject.videoeditor.silencedetection.remote;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.videoeditor.api.ProcessFailedException;
import org.opencastproject.videoeditor.silencedetection.api.SilenceDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class SilenceDetectionServiceRemote extends RemoteBase implements SilenceDetectionService {

  private static final Logger logger = LoggerFactory.getLogger(SilenceDetectionServiceRemote.class);
  private SmilService smilService;

  public SilenceDetectionServiceRemote() {
    super(JOB_TYPE);
  }

  @Override
  public Job detect(Track track) throws ProcessFailedException {
    HttpPost post = new HttpPost("/silencedetection");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    try {
      params.add(new BasicNameValuePair("track", MediaPackageElementParser.getAsXml(track)));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new ProcessFailedException(
              "Unable to assemble a remote silence detection request for track " + track.getIdentifier());
    }

    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        String entity = EntityUtils.toString(response.getEntity());
        if (StringUtils.isNotEmpty(entity)) {
          Job resultJob = JobParser.parseJob(entity);
          logger.info(
                  "Start silence detection for track '{}' on remote silence detection service",
                  track.getIdentifier());
          return resultJob;
        }
      }
    } catch (Exception e) {
      throw new ProcessFailedException("Unable to run silence detection for track "
              + track.getIdentifier() + " on remote silence detection service", e);
    } finally {
      closeConnection(response);
    }
    throw new ProcessFailedException("Unable to run silence detection for track "
            + track.getIdentifier() + " on remote silence detection service");
  }

  public void setSmilService(SmilService smilService) {
    this.smilService = smilService;
  }
}
