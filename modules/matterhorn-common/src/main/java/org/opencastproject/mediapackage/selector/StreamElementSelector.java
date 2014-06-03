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
package org.opencastproject.mediapackage.selector;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.Stream;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.TrackSupport;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This <code>MediaPackageElementSelector</code> selects all tracks from a <code>MediaPackage</code> that contain at
 * least an audio stream while optionally matching other requirements such as flavors and tags.
 */
public class StreamElementSelector<S extends Stream> extends AbstractMediaPackageElementSelector<Track> {

  /**
   * Creates a new selector.
   */
  public StreamElementSelector() {
  }

  /**
   * Creates a new selector that will restrict the result of <code>select()</code> to the given flavor.
   *
   * @param flavor
   *          the flavor
   */
  public StreamElementSelector(String flavor) {
    this(MediaPackageElementFlavor.parseFlavor(flavor));
  }

  /**
   * Creates a new selector that will restrict the result of <code>select()</code> to the given flavor.
   *
   * @param flavor
   *          the flavor
   */
  public StreamElementSelector(MediaPackageElementFlavor flavor) {
    addFlavor(flavor);
  }

  /**
   * Returns all tracks from a <code>MediaPackage</code> that contain at least a <code>Stream</code> of the parametrized
   * type while optionally matching other requirements such as flavors and tags. If no such combination can be found, i.
   * g. there is no audio or video at all, an empty array is returned.
   *
   * @see org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector#select(org.opencastproject.mediapackage.MediaPackage, boolean)
   */
  @SuppressWarnings("unchecked")
  @Override
  public Collection<Track> select(MediaPackage mediaPackage, boolean withTagsAndFlavors) {
    Collection<Track> candidates = super.select(mediaPackage, withTagsAndFlavors);
    List<Track> result = new ArrayList<Track>();
    for (Track t : candidates) {
      if (TrackSupport.byType(t.getStreams(), getParametrizedStreamType()).length > 0) {
        result.add(t);
      }
    }
    return result;
  }

  /**
   * This constructor tries to determine the entity type from the type argument used by a concrete implementation of
   * <code>GenericHibernateDao</code>.
   */
  @SuppressWarnings("unchecked")
  private Class getParametrizedStreamType() {
    Class current = getClass();
    Type superclass;
    Class<? extends S> entityClass = null;
    while ((superclass = current.getGenericSuperclass()) != null) {
      if (superclass instanceof ParameterizedType) {
        entityClass = (Class<S>) ((ParameterizedType) superclass).getActualTypeArguments()[0];
        break;
      } else if (superclass instanceof Class) {
        current = (Class) superclass;
      } else {
        break;
      }
    }
    if (entityClass == null) {
      throw new IllegalStateException("DAO creation exception: Cannot determine entity type because "
              + getClass().getName() + " does not specify any type parameter.");
    }
    return entityClass.getClass();
  }

}
