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

package org.opencastproject.distribution.streaming.remote;

import static java.lang.String.format;
import static org.opencastproject.util.HttpUtil.param;
import static org.opencastproject.util.HttpUtil.post;
import static org.opencastproject.util.JobUtil.jobFromHttpResponse;
import static org.opencastproject.util.data.functions.Options.join;

import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.StreamingDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.util.OsgiUtil;

import com.google.gson.Gson;

import org.apache.http.client.methods.HttpPost;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A remote distribution service invoker.
 */
public class StreamingDistributionServiceRemoteImpl extends RemoteBase implements StreamingDistributionService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(StreamingDistributionServiceRemoteImpl.class);

  /** The distribution channel identifier */
  protected String distributionChannel;

    /** The property to look up and append to REMOTE_SERVICE_TYPE_PREFIX */
  private static final String PARAM_CHANNEL_ID = "channelId";
  private static final String PARAM_MEDIAPACKAGE = "mediapackage";
  private static final String PARAM_ELEMENT_IDS = "elementIds";

  private static final Gson gson  = new Gson();

  public StreamingDistributionServiceRemoteImpl() {
    // the service type is not available at construction time. we need to wait for activation to set this value
    super("waiting for activation");
  }

  /** activates the component */
  protected void activate(ComponentContext cc) {
    this.distributionChannel = OsgiUtil.getComponentContextProperty(cc, CONFIG_KEY_STORE_TYPE);
    super.serviceType = JOB_TYPE_PREFIX + this.distributionChannel;
  }

  public String getDistributionType() {
    return this.distributionChannel;
  }

 @Override
  public Job distribute(String channelId, MediaPackage mediaPackage, String elementId)
          throws DistributionException, MediaPackageException {
    Set<String> elementIds = new HashSet<String>();
    elementIds.add(elementId);
    return distribute(channelId, mediaPackage, elementIds);
  }


  @Override
  public Job distribute(String channelId, final MediaPackage mediaPackage, Set<String> elementIds)
          throws DistributionException,  MediaPackageException {
    logger.info(format("Distributing %s elements to %s@%s", elementIds.size(), channelId, distributionChannel));
    final HttpPost req = post(param(PARAM_CHANNEL_ID, channelId),
                              param(PARAM_MEDIAPACKAGE, MediaPackageParser.getAsXml(mediaPackage)),
                              param(PARAM_ELEMENT_IDS, gson.toJson(elementIds)));
    for (Job job : join(runRequest(req, jobFromHttpResponse))) {
      return job;
    }
    throw new DistributionException(format("Unable to distribute '%s' elements of "
                                                   + "mediapackage '%s' using a remote destribution service proxy",
                                           elementIds.size(), mediaPackage.getIdentifier().toString()));
  }

  @Override
  public Job retract(String channelId, MediaPackage mediaPackage, String elementId) throws DistributionException {
    Set<String> elementIds = new HashSet<String>();
    elementIds.add(elementId);
    return retract(channelId, mediaPackage, elementIds);
  }

  @Override
  public Job retract(String channelId, MediaPackage mediaPackage, Set<String> elementIds) throws DistributionException {
    logger.info(format("Retracting %s elements from %s@%s", elementIds.size(), channelId, distributionChannel));
    final HttpPost req = post("/retract",
                              param(PARAM_MEDIAPACKAGE, MediaPackageParser.getAsXml(mediaPackage)),
                              param(PARAM_ELEMENT_IDS, gson.toJson(elementIds)),
                              param(PARAM_CHANNEL_ID, channelId));
    for (Job job : join(runRequest(req, jobFromHttpResponse))) {
      return job;
    }
    throw new DistributionException(format("Unable to retract '%s' elements of "
                                                   + "mediapackage '%s' using a remote destribution service proxy",
                                           elementIds.size(), mediaPackage.getIdentifier().toString()));
  }

  @Override
  public List<MediaPackageElement> distributeSync(String channelId, MediaPackage mediapackage, Set<String> elementIds)
      throws DistributionException {
    logger.info(format("Distributing %s elements to %s@%s", elementIds.size(), channelId, distributionChannel));
    final HttpPost req = post("/distributesync", param(PARAM_CHANNEL_ID, channelId),
        param(PARAM_MEDIAPACKAGE, MediaPackageParser.getAsXml(mediapackage)),
        param(PARAM_ELEMENT_IDS, gson.toJson(elementIds)));
    for (List<MediaPackageElement> elements : join(runRequest(req, elementsFromHttpResponse))) {
      return elements;
    }
    throw new DistributionException(format("Unable to distribute '%s' elements of "
            + "mediapackage '%s' using a remote destribution service proxy",
        elementIds.size(), mediapackage.getIdentifier().toString()));
  }

  @Override
  public List<MediaPackageElement> retractSync(String channelId, MediaPackage mediaPackage, Set<String> elementIds)
      throws DistributionException {
    logger.info(format("Retracting %s elements from %s@%s", elementIds.size(), channelId, distributionChannel));
    final HttpPost req = post("/retract",
        param(PARAM_MEDIAPACKAGE, MediaPackageParser.getAsXml(mediaPackage)),
        param(PARAM_ELEMENT_IDS, gson.toJson(elementIds)),
        param(PARAM_CHANNEL_ID, channelId));
    for (List<MediaPackageElement> elements : join(runRequest(req, elementsFromHttpResponse))) {
      return elements;
    }
    throw new DistributionException(format("Unable to retract '%s' elements of "
            + "mediapackage '%s' using a remote destribution service proxy",
        elementIds.size(), mediaPackage.getIdentifier().toString()));
  }
}
