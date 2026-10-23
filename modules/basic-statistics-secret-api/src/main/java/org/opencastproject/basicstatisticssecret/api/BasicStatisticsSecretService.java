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
package org.opencastproject.basicstatisticssecret.api;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public interface BasicStatisticsSecretService {

  /** Job type */
  String JOB_TYPE = "org.opencastproject.basicstatisticssecret";

  byte[] getCurrentSecret() throws BasicStatisticsSecretServiceException;

  /**
   * The next time it is 4am, which is when the secret is replaced every day. A secret that was obtained at the given
   * time must not be used anymore from the returned time on.
   */
  static Instant nextRotation(Instant now) {
    ZonedDateTime time = now.atZone(ZoneId.systemDefault());
    ZonedDateTime rotation = time.with(LocalTime.of(4, 0));
    return (rotation.isAfter(time) ? rotation : rotation.plusDays(1)).toInstant();
  }
}
