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

package org.opencastproject.dataloader;

import org.opencastproject.util.data.Option;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EventEntry {

  private final String title;
  private final Date recordingDate;
  private final int duration;
  private final boolean archive;
  private final Option<String> series;
  private final String captureAgent;
  private final String source;
  private final String contributor;
  private final Option<String> description;
  private final Option<String> seriesName;
  private final List<String> presenters = new ArrayList<String>();

  public EventEntry(String title, Date recordingDate, int duration, boolean archive, String series,
          String captureAgent, String source, String contributor, String description, String seriesName,
          List<String> presenters) {
    this.title = title;
    this.recordingDate = recordingDate;
    this.duration = duration;
    this.archive = archive;
    this.captureAgent = captureAgent;
    this.source = source;
    this.contributor = contributor;
    this.seriesName = Option.option(StringUtils.trimToNull(seriesName));
    this.series = Option.option(series);
    this.description = Option.option(StringUtils.trimToNull(description));
    this.presenters.addAll(presenters);
  }

  public String getTitle() {
    return title;
  }

  public Date getRecordingDate() {
    return recordingDate;
  }

  public int getDuration() {
    return duration;
  }

  public boolean isArchive() {
    return archive;
  }

  public Option<String> getSeries() {
    return series;
  }

  public Option<String> getSeriesName() {
    return seriesName;
  }

  public String getCaptureAgent() {
    return captureAgent;
  }

  public String getSource() {
    return source;
  }

  public String getContributor() {
    return contributor;
  }

  public Option<String> getDescription() {
    return description;
  }

  public List<String> getPresenters() {
    return presenters;
  }

}
