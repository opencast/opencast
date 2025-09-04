/*
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
package org.opencastproject.mediapackage;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class was created to allow more than one {@link MediaPackageSerializer} to be applied to the same
 * {@link MediaPackage}. For example if you enabled a redirect serializer to move urls from an old server to a new one
 * and a stream security serializer then the urls could be redirected and then signed.
 */
@Component(
    immediate = true,
    service = MediaPackageSerializer.class,
    property = {
        "service.pid=org.opencastproject.mediapackage.ChainingMediaPackageSerializer",
        "service.ranking:Integer=1000"
    }
)
public class ChainingMediaPackageSerializer implements MediaPackageSerializer {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ChainingMediaPackageSerializer.class);

  /** List of serializers ordered by their ranking */
  private List<MediaPackageSerializer> serializers = new ArrayList<MediaPackageSerializer>();

  /** This serializer should never be chained again and zero as a neutral ranking therefore seems to be appropriate */
  public static final int RANKING = 0;

  /** OSGi DI */
  @Reference(
      cardinality = ReferenceCardinality.MULTIPLE,
      policy = ReferencePolicy.DYNAMIC,
      unbind = "removeMediaPackageSerializer",
      target = "(!(service.pid=org.opencastproject.mediapackage.ChainingMediaPackageSerializer))"
  )
  void addMediaPackageSerializer(MediaPackageSerializer serializer) {
    serializers.add(serializer);
    Collections.sort(serializers, new Comparator<MediaPackageSerializer>() {
      @Override
      public int compare(MediaPackageSerializer o1, MediaPackageSerializer o2) {
        return o1.getRanking() - o2.getRanking();
      }
    });
    logger.info("MediaPackageSerializer '{}' with ranking {} added to serializer chain.", serializer,
            serializer.getRanking());
  }

  /** OSGi DI */
  void removeMediaPackageSerializer(MediaPackageSerializer serializer) {
    serializers.remove(serializer);
    logger.info("MediaPackageSerializer '{}' with ranking {} removed from serializer chain.", serializer,
            serializer.getRanking());
  }

  @Override
  public URI encodeURI(URI uri) throws URISyntaxException {
    URI result = uri;
    // Reverse the serializers list and apply encodeURI one by one
    for (int i = serializers.size() - 1; i >= 0; i--) {
      MediaPackageSerializer serializer = serializers.get(i);
      try {
        result = serializer.encodeURI(result);
      } catch (URISyntaxException e) {
        logger.warn("Error while encoding URI with serializer '{}':", serializer, e);
        throw e;
      }
    }
    return result;
  }

  @Override
  public URI decodeURI(URI uri) throws URISyntaxException {
    URI result = uri;
    // Apply decodeURI in order
    for (MediaPackageSerializer serializer : serializers) {
      try {
        result = serializer.decodeURI(result);
      } catch (URISyntaxException e) {
        logger.warn("Error while encoding URI with serializer '{}':", serializer, e);
        throw e;
      }
    }
    return result;
  }

  @Override
  public int getRanking() {
    return RANKING;
  }

}
