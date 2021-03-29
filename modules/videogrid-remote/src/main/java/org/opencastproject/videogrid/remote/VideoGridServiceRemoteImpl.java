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

package org.opencastproject.videogrid.remote;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.videogrid.api.VideoGridService;
import org.opencastproject.videogrid.api.VideoGridServiceException;

import com.google.gson.Gson;

import org.apache.commons.codec.EncoderException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VideoGridServiceRemoteImpl extends RemoteBase implements VideoGridService {

  private static final Logger logger = LoggerFactory.getLogger(VideoGridServiceRemoteImpl.class);

  private static final Gson gson = new Gson();

  /** Creates a new videogrid service instance. */
  public VideoGridServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  @Override
  public Job createPartialTracks(List<List<String>> commands, Track... tracks)
          throws VideoGridServiceException, EncoderException {

    // serialize arguments and metadata
    String commandsJson = gson.toJson(commands);

    // Build form parameters
    List<NameValuePair> params = new ArrayList<>();
    try {
      params.add(new BasicNameValuePair("commands", commandsJson));
      params.add(
              new BasicNameValuePair("sourceTracks", MediaPackageElementParser.getArrayAsXml(Arrays.asList(tracks))));
    } catch (Exception e) {
      throw new EncoderException(e);
    }

    logger.info("Video-gridding {}", commandsJson);
    HttpResponse response = null;
    try {
      HttpPost post = new HttpPost("/videogrid");
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
      response = getResponse(post);
      if (response == null) {
        throw new VideoGridServiceException("No response from service");
      }
      Job receipt = JobParser.parseJob(response.getEntity().getContent());
      logger.info("Completed video-gridding {}", commands);
      return receipt;
    } catch (IOException e) {
      throw new VideoGridServiceException("Failed building service request", e);
    } finally {
      closeConnection(response);
    }
  }
}
