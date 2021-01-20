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

package org.opencastproject.ingestdownloadservice.remote;

import org.opencastproject.ingestdownloadservice.api.IngestDownloadService;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Remote implementation of the ingest download service
 */
public class IngestDownloadServiceRemoteImpl extends RemoteBase implements IngestDownloadService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(IngestDownloadServiceRemoteImpl.class);


  /**
   * Constructs a new ingest download service remote
   */
  public IngestDownloadServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  /**
   *
   * @param mediaPackage
   *        The media package to download elements from
   * @param sourceFlavors
   *        Flavors identifying elements to download
   * @param sourceTags
   *        Tags identifying elements to download
   * @param deleteExternal
   *        If the service should try to delete external elements after downloading
   * @param tagsAndFlavor
   *        If elements are selected based on a union or an interjection of the sets selected by tags and flavors
   * @return The launched job
   */
  @Override
  public Job ingestDownload(MediaPackage mediaPackage, String sourceFlavors, String sourceTags, boolean deleteExternal,
          boolean tagsAndFlavor) throws ServiceRegistryException {
    HttpResponse response = null;

    try {
      final String mediaPackageStr = MediaPackageParser.getAsXml(mediaPackage);
      List<NameValuePair> formStringParams = new ArrayList<>();
      formStringParams.add(new BasicNameValuePair("mediapackage", mediaPackageStr));
      formStringParams.add(new BasicNameValuePair("sourceFlavors", sourceFlavors));
      formStringParams.add(new BasicNameValuePair("sourceTags", sourceTags));
      formStringParams.add(new BasicNameValuePair("deleteExternal", Boolean.toString(deleteExternal)));
      formStringParams.add(new BasicNameValuePair("tagsAndFlavor", Boolean.toString(tagsAndFlavor)));

      logger.info("Downloading Source form mediapackge: {} to workspace", mediaPackage.getIdentifier());

      final HttpPost post = new HttpPost("/ingestdownload");
      post.setEntity(new UrlEncodedFormEntity(formStringParams, "UTF-8"));
      response = getResponse(post);

      if (response != null) {
        Job job = JobParser.parseJob(response.getEntity().getContent());
        logger.info("Starting to download into workspace on remote IngestDownload {}",
                mediaPackage.getIdentifier().toString());
        return job;
      }
      throw new ServiceRegistryException("Failed to start remote IngestDownload " + mediaPackage.getIdentifier());
    } catch (IOException e) {
      throw new ServiceRegistryException("Failed to start remote IngestDownload " + mediaPackage.getIdentifier(), e);
    } finally {
      closeConnection(response);
    }
  }
}

