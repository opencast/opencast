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
package org.opencastproject.publication.configurable.remote;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.publication.api.ConfigurablePublicationService;
import org.opencastproject.publication.api.PublicationException;
import org.opencastproject.serviceregistry.api.RemoteBase;

import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A remote publication service invoker.
 */
public class ConfigurablePublicationServiceRemoteImpl extends RemoteBase implements ConfigurablePublicationService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ConfigurablePublicationServiceRemoteImpl.class);

  /* Gson is thread-safe so we use a single instance */
  private Gson gson = new Gson();

  public ConfigurablePublicationServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  @Override
  public Job replace(final MediaPackage mediaPackage, final String channelId,
          final Collection<? extends MediaPackageElement> addElements, final Set<String> retractElementIds)
              throws PublicationException, MediaPackageException {

    final List<BasicNameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediaPackage)));
    params.add(new BasicNameValuePair("channel", channelId));
    params.add(new BasicNameValuePair("addElements", MediaPackageElementParser.getArrayAsXml(addElements)));
    params.add(new BasicNameValuePair("retractElements", gson.toJson(retractElementIds)));

    final HttpPost post = new HttpPost("/replace");
    HttpResponse response = null;
    try {
      post.setEntity(new UrlEncodedFormEntity(params, UTF_8));
      response = getResponse(post);
      if (response != null) {
        logger.info("Publishing media package {} to channel {} using a remote publication service",
                mediaPackage, channelId);
        try {
          return JobParser.parseJob(response.getEntity().getContent());
        } catch (final Exception e) {
          throw new PublicationException(
                  "Unable to publish media package '" + mediaPackage + "' using a remote publication service", e);
        }
      }
    } catch (final Exception e) {
      throw new PublicationException(
              "Unable to publish media package " + mediaPackage + " using a remote publication service.", e);
    } finally {
      closeConnection(response);
    }
    throw new PublicationException(
            "Unable to publish mediapackage " + mediaPackage + " using a remote publication service.");
  }

  @Override
  public Publication replaceSync(
      MediaPackage mediaPackage, String channelId, Collection<? extends MediaPackageElement> addElements,
      Set<String> retractElementIds) throws PublicationException, MediaPackageException {
    final List<BasicNameValuePair> params = new ArrayList<>();
    params.add(new BasicNameValuePair("mediapackage", MediaPackageParser.getAsXml(mediaPackage)));
    params.add(new BasicNameValuePair("channel", channelId));
    params.add(new BasicNameValuePair("addElements", MediaPackageElementParser.getArrayAsXml(addElements)));
    params.add(new BasicNameValuePair("retractElements", gson.toJson(retractElementIds)));

    final HttpPost post = new HttpPost("/replacesync");
    HttpResponse response = null;
    try {
      post.setEntity(new UrlEncodedFormEntity(params, UTF_8));
      response = getResponse(post);
      if (response != null) {
        logger.info("Publishing media package {} to channel {} using a remote publication service",
            mediaPackage, channelId);
        try {
          final String xml = IOUtils.toString(response.getEntity().getContent(), Charset.forName("utf-8"));
          return (Publication) MediaPackageElementParser.getFromXml(xml);
        } catch (final Exception e) {
          throw new PublicationException(
              "Unable to publish media package '" + mediaPackage + "' using a remote publication service", e);
        }
      }
    } catch (final Exception e) {
      throw new PublicationException(
          "Unable to publish media package " + mediaPackage + " using a remote publication service.", e);
    } finally {
      closeConnection(response);
    }
    throw new PublicationException(
        "Unable to publish mediapackage " + mediaPackage + " using a remote publication service.");
  }
}
