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
package org.opencastproject.videosegmenter.remote;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.videosegmenter.api.VideoSegmenterException;
import org.opencastproject.videosegmenter.api.VideoSegmenterService;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class VideoSegmenterRemoteImpl extends RemoteBase implements VideoSegmenterService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(VideoSegmenterRemoteImpl.class);

  public VideoSegmenterRemoteImpl() {
    super(JOB_TYPE);
  }

  @Override
  public Job segment(Track track) throws VideoSegmenterException {
    HttpPost post = new HttpPost();
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("track", MediaPackageElementParser.getAsXml(track)));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new VideoSegmenterException(e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        try {
          Job receipt = JobParser.parseJob(response.getEntity().getContent());
          logger.info("Analyzing {} on a remote analysis server", track);
          return receipt;
        } catch (Exception e) {
          throw new VideoSegmenterException(
                  "Unable to analyze element '" + track + "' using a remote analysis service", e);
        }
      }
    } finally {
      closeConnection(response);
    }
    throw new VideoSegmenterException("Unable to analyze element '" + track + "' using a remote analysis service");
  }

}
