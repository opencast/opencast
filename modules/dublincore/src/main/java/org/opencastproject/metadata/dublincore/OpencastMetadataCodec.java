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
package org.opencastproject.metadata.dublincore;

import com.entwinemedia.fn.Fn;

import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Collection of metadata encoding functions following Opencast rules.
 */
@ParametersAreNonnullByDefault
public final class OpencastMetadataCodec {
  private OpencastMetadataCodec() {
  }

  /** Encode a date with day precision. */
  @Nonnull public static DublinCoreValue encodeDate(Date date) {
    return EncodingSchemeUtils.encodeDate(date, Precision.Day);
  }

  /** Encode a date with a given precision. */
  @Nonnull public static DublinCoreValue encodeDate(Date date, Precision p) {
    return EncodingSchemeUtils.encodeDate(date, p);
  }

  /** Decode a W3C-DTF encoded date. */
  @Nonnull public static Date decodeDate(String date) {
    return EncodingSchemeUtils.decodeMandatoryDate(date);
  }

  /** {@link OpencastMetadataCodec#decodeDate(java.lang.String)} as a function. */
  public static final Fn<String, Date> decodeDate = new Fn<String, Date>() {
    @Override public Date apply(String a) {
      return decodeDate(a);
    }
  };

  /** Encode a duration. */
  @Nonnull public static DublinCoreValue encodeDuration(long ms) {
    return EncodingSchemeUtils.encodeDuration(ms);
  }

  /** Decode a duration. */
  public static long decodeDuration(String ms) {
    return EncodingSchemeUtils.decodeMandatoryDuration(ms);
  }

  /** {@link OpencastMetadataCodec#decodeDuration(String)} as a function. */
  public static final Fn<String, Long> decodeDuration = new Fn<String, Long>() {
    @Override public Long apply(String a) {
      return decodeDuration(a);
    }
  };

  /** Encode a period with a given precision. */
  @Nonnull public static DublinCoreValue encodePeriod(Date from, Date to, Precision precision) {
    return EncodingSchemeUtils.encodePeriod(new DCMIPeriod(from, to), precision);
  }

  /** Decode a period. */
  @Nonnull public static DCMIPeriod decodePeriod(String period) {
    return EncodingSchemeUtils.decodeMandatoryPeriod(period);
  }

  /** Decode a temporal value. */
  @Nonnull public static Temporal decodeTemporal(DublinCoreValue temporal) {
    return EncodingSchemeUtils.decodeMandatoryTemporal(temporal);
  }

  /** {@link OpencastMetadataCodec#decodeTemporal(DublinCoreValue)} as a function. */
  @Nonnull public static final Fn<DublinCoreValue, Temporal> decodeTemporal = new Fn<DublinCoreValue, Temporal>() {
    @Override public Temporal apply(DublinCoreValue a) {
      return decodeTemporal(a);
    }
  };
}
