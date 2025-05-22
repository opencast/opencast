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
package org.opencastproject.editor.api;

import static java.util.Objects.requireNonNull;

import org.opencastproject.security.api.User;
import org.opencastproject.util.data.Tuple;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Provides access to the parsed editing information
 */
public class EditingData {
  public static final String WORKFLOW_ACTIVE = "workflow_active";
  public static final String LOCKING_ACTIVE = "locking_active";
  public static final String LOCK_REFRESH = "lock_refresh";
  public static final String LOCK_UUID = "lock_uuid";
  public static final String LOCK_USER = "lock_user";
  private final List<SegmentData> segments;
  private final List<WorkflowData> workflows;
  private final List<TrackData> tracks;
  private final String title;
  private final String date;
  private final Long duration;
  private final SeriesData series;
  @SerializedName(WORKFLOW_ACTIVE)
  private final Boolean workflowActive;
  @SerializedName(LOCKING_ACTIVE)
  private final Boolean lockingActive;
  @SerializedName(LOCK_REFRESH)
  private final Integer lockRefresh;
  @SerializedName(LOCK_UUID)
  private final String lockUUID;
  @SerializedName(LOCK_USER)
  private final String lockUser;

  private final List<String> waveformURIs;
  private final List<Subtitle> subtitles;
  private final Boolean local;

  private final String metadataJSON;

  public EditingData(List<SegmentData> segments, List<TrackData> tracks, List<WorkflowData> workflows, Long duration,
          String title, String recordingStartDate, String seriesId, String seriesName, Boolean workflowActive,
          List<String> waveformURIs, List<Subtitle> subtitles, Boolean local, Boolean lockingActive,
          Integer lockRefresh, User user, String metadataJSON) {
    this.segments = segments;
    this.tracks = tracks;
    this.workflows = workflows;
    this.duration = duration;
    this.title = title;
    this.date = recordingStartDate;
    this.series = new SeriesData(seriesId, seriesName);
    this.workflowActive = workflowActive;
    this.waveformURIs = waveformURIs;
    this.subtitles = subtitles;
    this.local = local;
    this.lockingActive = lockingActive;
    this.lockRefresh = lockRefresh * 1000;
    this.lockUUID = UUID.randomUUID().toString();
    this.lockUser = user.getUsername();
    this.metadataJSON = metadataJSON;
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

  public List<Subtitle> getSubtitles() {
    return subtitles;
  }

  public String getMetadataJSON() {
    return metadataJSON;
  }

  public String toString() {
    Gson gson = new GsonBuilder().serializeNulls().create();
    return gson.toJson(this);
  }

  public static final class Subtitle {
    private final String id;
    /** content of the subtitle */
    private final String subtitle;
    private final String[] tags;
    private final boolean deleted;

    public Subtitle(String id, String subtitle, String[] tags) {
      this(id, subtitle, tags,false);
    }

    public Subtitle(String id, String subtitle, String[] tags, boolean deleted) {
      this.id = id;
      this.subtitle = subtitle;
      this.tags = tags;
      this.deleted = deleted;
    }

    public String getId() {
      return id;
    }

    public String getSubtitle() {
      return subtitle;
    }

    public String[] getTags() {
      return tags;
    }

    public boolean isDeleted() {
      return deleted;
    }

  }
}

