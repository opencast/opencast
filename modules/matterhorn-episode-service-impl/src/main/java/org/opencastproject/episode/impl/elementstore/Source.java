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
package org.opencastproject.episode.impl.elementstore;

import org.opencastproject.util.MimeType;
import org.opencastproject.util.data.Option;

import java.net.URI;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

/** A data source along with optional content hints. */
public final class Source {
  private final URI uri;
  private final Option<Long> size;
  private final Option<MimeType> mimeType;

  public Source(URI uri, Option<Long> size, Option<MimeType> mimeType) {
    this.uri = uri;
    this.size = size;
    this.mimeType = mimeType;
  }

  /** Create a new source. */
  public static Source source(URI uri) {
    return new Source(uri, none(0L), none(MimeType.class));
  }

  /** Create a new source. */
  public static Source source(URI uri, Long size) {
    return new Source(uri, some(size), none(MimeType.class));
  }

  /** Create a new source. */
  public static Source source(URI uri, Long size, MimeType mimeType) {
    return new Source(uri, some(size), some(mimeType));
  }

  /** Create a new source. */
  public static Source source(URI uri, Option<Long> size, Option<MimeType> mimeType) {
    return new Source(uri, size, mimeType);
  }

  public URI getUri() {
    return uri;
  }

  public Option<Long> getSize() {
    return size;
  }

  public Option<MimeType> getMimeType() {
    return mimeType;
  }
}
