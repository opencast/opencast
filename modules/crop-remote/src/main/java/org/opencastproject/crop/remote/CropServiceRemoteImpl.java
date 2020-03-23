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
package org.opencastproject.crop.remote;

import org.opencastproject.crop.api.CropException;
import org.opencastproject.crop.api.CropService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
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
 * Remote implementation for {@link CropService}
 */
public class CropServiceRemoteImpl extends RemoteBase implements CropService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(CropServiceRemoteImpl.class);

  public CropServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  @Override
  public Job crop(Track track) throws CropException, MediaPackageException {
    HttpPost post = new HttpPost("/");
    try {
      List<BasicNameValuePair> params = new ArrayList<>();
      params.add(new BasicNameValuePair("track", MediaPackageElementParser.getAsXml(track)));
      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new CropException("Unable to assemble a remote crop request for track" + track, e);
    }
    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        String content = EntityUtils.toString(response.getEntity());
        Job job = JobParser.parseJob(content);
        logger.info("Crop job {} started", job.getId());
        return job;
      }
    } catch (Exception e) {
      throw new CropException("Unable to crop track" + track + " using remote crop service", e);
    } finally {
      closeConnection(response);
    }
    throw new CropException("Unable to crop track" + track + " using remote crop service");
  }
}
