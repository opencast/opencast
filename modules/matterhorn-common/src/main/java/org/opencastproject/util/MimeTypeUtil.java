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

package org.opencastproject.util;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

/** Utility functions for mime types. */
public final class MimeTypeUtil {
  private MimeTypeUtil() {
  }

  /** {@link org.opencastproject.util.MimeType#getSuffix()} as a function. */
  public static final Function<MimeType, Option<String>> suffix = new Function<MimeType, Option<String>>() {
    @Override
    public Option<String> apply(MimeType mimeType) {
      return mimeType.getSuffix();
    }
  };

  /** {@link org.opencastproject.util.MimeType#toString()} as a function. */
  public static final Function<MimeType, String> toString = new Function<MimeType, String>() {
    @Override
    public String apply(MimeType mimeType) {
      return mimeType.toString();
    }
  };

}
