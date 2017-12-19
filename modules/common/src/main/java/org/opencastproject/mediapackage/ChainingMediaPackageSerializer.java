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
package org.opencastproject.mediapackage;

import static com.entwinemedia.fn.Prelude.chuck;
import static com.entwinemedia.fn.Stream.$;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

import com.entwinemedia.fn.Fn2;

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
public class ChainingMediaPackageSerializer implements MediaPackageSerializer {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ChainingMediaPackageSerializer.class);

  /** List of serializers ordered by their ranking */
  private List<MediaPackageSerializer> serializers = new ArrayList<MediaPackageSerializer>();

  /** This serializer should never be chained again and zero as a neutral ranking therefore seems to be appropriate */
  public static final int RANKING = 0;

  /** OSGi DI */
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
    return $(serializers).reverse().foldl(uri, new Fn2<URI, MediaPackageSerializer, URI>() {
      @Override
      public URI apply(URI uri, MediaPackageSerializer serializer) {
        try {
          return serializer.encodeURI(uri);
        } catch (URISyntaxException e) {
          logger.warn("Error while encoding URI with serializer '{}': {}", serializer, getStackTrace(e));
          return chuck(e);
        }
      }
    });
  }

  @Override
  public URI decodeURI(URI uri) throws URISyntaxException {
    return $(serializers).foldl(uri, new Fn2<URI, MediaPackageSerializer, URI>() {
      @Override
      public URI apply(URI uri, MediaPackageSerializer serializer) {
        try {
          return serializer.decodeURI(uri);
        } catch (URISyntaxException e) {
          logger.warn("Error while encoding URI with serializer '{}': {}", serializer, getStackTrace(e));
          return chuck(e);
        }
      }
    });
  }

  @Override
  public int getRanking() {
    return RANKING;
  }

}
