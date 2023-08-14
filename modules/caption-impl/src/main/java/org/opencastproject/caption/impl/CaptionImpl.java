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

package org.opencastproject.caption.impl;

import org.opencastproject.caption.api.Caption;
import org.opencastproject.caption.api.Time;

/**
 * Implementation of {@link Caption}. Caption text is stored in array where each element represents one caption line.
 *
 */
public class CaptionImpl implements Caption {

  private Time startTime;
  private Time stopTime;
  private String[] captionLines;

  public CaptionImpl(Time startTime, Time stopTime, String[] captionLines) {
    this.startTime = startTime;
    this.stopTime = stopTime;
    this.captionLines = captionLines;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getCaption() {
    return captionLines;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Time getStartTime() {
    return startTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Time getStopTime() {
    return stopTime;
  }
}
