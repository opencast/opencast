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

package org.opencastproject.archive.api;

import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;

import java.net.URI;

/**
 * Create a URI for a media package element. Please <em>do not</em> modify the given media package element
 * but return the rewritten URI instead.
 */
public abstract class UriRewriter extends Function2<Version, MediaPackageElement, URI> {
  /** Convert a function into a UriRewriter. */
  public static UriRewriter fromFunction(final Function2<Version, MediaPackageElement, URI> f) {
    return new UriRewriter() {
      @Override public URI apply(Version v, MediaPackageElement mpe) {
        return f.apply(v, mpe);
      }
    };
  }

  /** {@link UriRewriter#fromFunction(org.opencastproject.util.data.Function2)} as a function. */
  public static final Function<Function2<Version, MediaPackageElement, URI>, UriRewriter> fromFunction =
          new Function<Function2<Version, MediaPackageElement, URI>, UriRewriter>() {
            @Override
            public UriRewriter apply(Function2<Version, MediaPackageElement, URI> f) {
              return fromFunction(f);
            }
          };
}
