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

import static com.entwinemedia.fn.data.json.Jsons.arr;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static java.util.Objects.requireNonNull;

import org.opencastproject.util.data.Tuple;

import com.entwinemedia.fn.data.json.Jsons;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides access to the parsed editing information
 */
public final class EditingData {

  public static final String TITLE = "title";
  public static final String DATE = "date";
  public static final String SERIES = "series";
  public static final String DURATION = "duration";
  public static final String SEGMENTS = "segments";
  public static final String TRACKS = "tracks";
  public static final String WORKFLOWS = "workflows";
  public static final String SERIES_ID = "id";
  public static final String SERIES_NAME = "title";

  private final List<SegmentData> segments;
  private final List<WorkflowData> workflows;
  private final List<TrackData> tracks;
  private final String title;
  private final String recordingStartDate;
  private final Long duration;
  private final String seriesId;
  private final String seriesName;

  public EditingData(List<SegmentData> segments, List<TrackData> tracks,
          List<WorkflowData> workflows, Long duration, String title, String recordingStartDate, String seriesId,
          String seriesName) {
    this.segments = segments;
    this.tracks = tracks;
    this.workflows = workflows;
    this.duration = duration;
    this.title = title;
    this.recordingStartDate = recordingStartDate;
    this.seriesId = seriesId;
    this.seriesName = seriesName;
  }

  /**
   * Parse {@link JSONObject} to {@link EditingData}.
   *
   * @param json the JSON object to parse
   * @return all editing information found in the JSON object
   */
  public static EditingData parse(String json) {
    requireNonNull(json);

    JSONObject jsonEditData = null;
    try {
      jsonEditData = (JSONObject) new JSONParser().parse(json);
    } catch (ParseException e) {
      throw new IllegalArgumentException("Unable to parse: '" + json + "' as json");
    }
    JSONArray jsonSegments = requireNonNull((JSONArray) jsonEditData.get(SEGMENTS));
    JSONArray jsonTracks = requireNonNull((JSONArray) jsonEditData.get(TRACKS));
    JSONArray jsonWorkflows = (JSONArray) jsonEditData.get(WORKFLOWS);

    List<SegmentData> segments = new ArrayList<>();
    for (Object segment : jsonSegments) {
      SegmentData segmentData = SegmentData.parse((JSONObject)segment);
      if (segmentData != null) {
        segments.add(segmentData);
      }
    }

    List<TrackData> tracks = new ArrayList<>();
    for (Object sourceTrack : jsonTracks) {
      TrackData trackData = TrackData.parse((JSONObject) sourceTrack);
      if (trackData != null) {
        tracks.add(trackData);
      }
    }

    List<WorkflowData> workflows = new ArrayList<>();
    if (jsonWorkflows != null) {
      for (Object workflow : jsonWorkflows) {
        WorkflowData workflowData = WorkflowData.parse((JSONObject) workflow);
        if (workflowData != null) {
          workflows.add(workflowData);
        }
      }
    }

    String seriesId = null;
    String seriesName = null;
    JSONObject series = (JSONObject) jsonEditData.get(SERIES);
    if (series != null) {
      seriesId = (String) series.get(SERIES_ID);
      seriesName = (String) series.get(SERIES_NAME);
    }

    Object jsonDuration = jsonEditData.get(DURATION);
    Long duration;
    if (jsonDuration == null) {
      duration = null;
    } else if (jsonDuration instanceof String) {
      duration = Long.decode((String) jsonDuration);
    } else if (jsonDuration instanceof Long) {
      duration = (Long) jsonDuration;
    } else {
      throw new IllegalArgumentException("Unable to decode duration");
    }

    return new EditingData(segments, tracks, workflows, duration, (String) jsonEditData.get(TITLE),
            (String) jsonEditData.get(DATE), seriesId, seriesName);
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
    return (workflows.size() > 0) ? workflows.get(0).getId() : null;
  }

  public List<TrackData> getTracks() {
    return Collections.unmodifiableList(tracks);
  }

  public String toString() {
    return obj(f(TITLE, v(title, Jsons.NULL)),
            f(DATE, v(recordingStartDate, Jsons.NULL)),
            f(SERIES, obj(f(SERIES_ID, v(seriesId, Jsons.NULL)), f(SERIES_NAME, v(seriesName, Jsons.NULL)))),
            f(TRACKS, arr(tracks.stream().map(TrackData::toJson).collect(Collectors.toList()))),
            f(DURATION, v(duration, Jsons.NULL)),
            f(SEGMENTS, arr(segments.stream().map(SegmentData::toJson).collect(Collectors.toList()))),
            f(WORKFLOWS, arr(workflows.stream().map(WorkflowData::toJson).collect(Collectors.toList())))).toString();
  }
}

