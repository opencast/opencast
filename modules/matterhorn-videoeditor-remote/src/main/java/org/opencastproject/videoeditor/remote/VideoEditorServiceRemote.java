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

package org.opencastproject.videoeditor.remote;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.videoeditor.api.ProcessFailedException;
import org.opencastproject.videoeditor.api.VideoEditorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Video editor service proxy for use as a JVM local service.
 */
public class VideoEditorServiceRemote extends RemoteBase implements VideoEditorService {

  private static final Logger logger = LoggerFactory.getLogger(VideoEditorServiceRemote.class);

  public VideoEditorServiceRemote() {
    super(JOB_TYPE);
  }

  @Override
  public List<Job> processSmil(Smil smil) throws ProcessFailedException {
    HttpPost post = new HttpPost("/process-smil");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    try {
      params.add(new BasicNameValuePair("smil", smil.toXML()));
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (Exception e) {
      throw new ProcessFailedException(
              "Unable to assemble a remote videoeditor request for smil " + smil.getId());
    }

    HttpResponse response = null;
    try {
      response = getResponse(post);
      if (response != null) {
        String entity = EntityUtils.toString(response.getEntity());
        if (StringUtils.isNotEmpty(entity)) {
          List<Job> jobs = new LinkedList<Job>();
          for (Job job : JobParser.parseJobList(entity).getJobs()) {
            jobs.add(job);
          }
          logger.info(
                  "Start proccessing smil '{}' on remote videoeditor service", smil.getId());
          return jobs;
        }
      }
    } catch (Exception e) {
      throw new ProcessFailedException("Unable to proccess smil "
              + smil.getId() + " using a remote videoeditor service", e);
    } finally {
      closeConnection(response);
    }
    throw new ProcessFailedException("Unable to proccess smil "
            + smil.getId() + " using a remote videoeditor service.");
  }
}

