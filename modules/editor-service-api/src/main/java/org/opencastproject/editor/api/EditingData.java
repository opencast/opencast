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
package org.opencastproject.editor.api;

import static java.util.Objects.requireNonNull;

import org.opencastproject.util.data.Tuple;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Collections;
import java.util.List;

/**
 * Provides access to the parsed editing information
 */
public final class EditingData {
  private final List<SegmentData> segments;
  private final List<WorkflowData> workflows;
  private final List<TrackData> tracks;
  private final String title;
  private final String date;
  private final Long duration;
  private final SeriesData series;

  public EditingData(List<SegmentData> segments, List<TrackData> tracks,
          List<WorkflowData> workflows, Long duration, String title, String recordingStartDate, String seriesId,
          String seriesName) {
    this.segments = segments;
    this.tracks = tracks;
    this.workflows = workflows;
    this.duration = duration;
    this.title = title;
    this.date = recordingStartDate;
    this.series = new SeriesData(seriesId, seriesName);
  }

  public static EditingData parse(String json) {
    requireNonNull(json);
    Gson gson = new Gson();
    EditingData editingData = gson.fromJson(json, EditingData.class);
    requireNonNull(editingData.getTracks());
    requireNonNull(editingData.getSegments());

    return editingData;
  }

  /**
   * Returns a list of {@link Tuple} that each represents a segment. {@link Tuple#getA()} marks the start point,
   * {@link Tuple#getB()} the endpoint of the segement.
   */
  public List<SegmentData> getSegments() {
    return Collections.unmodifiableList(segments);
  }

   /**
   * Returns the optional workflow to start
   */
  public String getPostProcessingWorkflow() {
    return (workflows != null && workflows.size() > 0) ? workflows.get(0).getId() : null;
  }

  public List<TrackData> getTracks() {
    return Collections.unmodifiableList(tracks);
  }

  public String toString() {
    Gson gson = new GsonBuilder().serializeNulls().create();
    return gson.toJson(this);
  }
}

