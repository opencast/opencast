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
package org.opencastproject.assetmanager.impl.persistence;

import static org.opencastproject.util.data.functions.Functions.chuck;

import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.MimeTypes;

import com.entwinemedia.fn.data.Opt;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Simple business type conversions to and from persistence types.
 */
@ParametersAreNonnullByDefault
public final class Conversions {
  private Conversions() {
  }

  public static VersionImpl toVersion(long a) {
    return VersionImpl.mk(a);
  }

  public static MediaPackage toMediaPackage(String xml) {
    try {
      return MediaPackageParser.getFromXml(xml);
    } catch (MediaPackageException e) {
      return chuck(e);
    }
  }

  public static Opt<MimeType> toMimeType(@Nullable String a) {
    if (a != null) {
      try {
        return Opt.some(MimeTypes.parseMimeType(a));
      } catch (Exception e) {
        return Opt.none();
      }
    } else {
      return Opt.none();
    }
  }
}
