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
package org.opencastproject.assetmanager.api.storage;

import org.opencastproject.util.MimeType;

import java.net.URI;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

/** 
 * A data source along with some optional content hints.
 */
@ParametersAreNonnullByDefault
public final class Source {
  private final URI uri;
  private final Optional<Long> size;
  private final Optional<MimeType> mimeType;

  public Source(URI uri, Optional<Long> size, Optional<MimeType> mimeType) {
    this.uri = uri;
    this.size = size;
    this.mimeType = mimeType;
  }

  /** Create a new source. */
  public static Source mk(URI uri) {
    return new Source(uri, Optional.<Long>empty(), Optional.<MimeType>empty());
  }

  /** Create a new source. */
  public static Source mk(URI uri, Long size) {
    return new Source(uri, Optional.of(size), Optional.<MimeType>empty());
  }

  /** Create a new source. */
  public static Source mk(URI uri, Long size, MimeType mimeType) {
    return new Source(uri, Optional.of(size), Optional.of(mimeType));
  }

  /** Create a new source. */
  public static Source mk(URI uri, Optional<Long> size, Optional<MimeType> mimeType) {
    return new Source(uri, size, mimeType);
  }

  public URI getUri() {
    return uri;
  }

  public Optional<Long> getSize() {
    return size;
  }

  public Optional<MimeType> getMimeType() {
    return mimeType;
  }
}
