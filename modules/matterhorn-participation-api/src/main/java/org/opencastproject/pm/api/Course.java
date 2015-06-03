/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.pm.api;

import static org.opencastproject.util.RequireUtil.notEmpty;
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Business object for a course.
 */
public class Course {

  /** The course identifier */
  private Long id;

  /** The syllabus course identifier */
  private String courseId;

  /** The series identifier */
  private String seriesId;

  /** The course name */
  private String name;

  /** The course description */
  private String description;

  /** The scheduling schedulingSource */
  private Option<SchedulingSource> schedulingSource;

  /** The external course key */
  private String externalCourseKey;

  /** The fingerprint */
  private Option<String> fingerprint;

  public Course(Long id, String courseId, String seriesId, String name, String description, String externalCourseKey,
          Option<String> fingerprint) {
    this.id = id;
    this.courseId = courseId;
    this.seriesId = seriesId;
    this.name = name;
    this.description = description;
    this.externalCourseKey = externalCourseKey;
    this.fingerprint = fingerprint;
    this.schedulingSource = Option.none(SchedulingSource.class);
  }

  /**
   * Constructor with only courseId
   *
   * @param courseId
   *          the syllabus+ identifier for this course
   */
  public Course(String courseId) {
    this.courseId = notEmpty(courseId, "courseId");
    this.fingerprint = Option.none(String.class);
    this.schedulingSource = Option.none(SchedulingSource.class);
  }

  /**
   * Constructor with some parameters
   *
   * @param courseId
   *          the syllabus+ course identifier
   * @param name
   *          the course name
   * @param description
   *          the course description
   */
  public Course(String courseId, String name, String description) {
    this(courseId, null, name, description, null);
  }

  /**
   * Constructor with all parameters
   *
   * @param courseId
   *          the syllabus+ course identifier
   * @param seriesId
   *          the matterhorn series identifier
   * @param name
   *          the course name
   * @param description
   *          the course description
   */
  public Course(String courseId, String seriesId, String name, String description) {
    this(courseId, seriesId, name, description, null);
  }

  /**
   * Constructor with all parameters
   *
   * @param courseId
   *          the syllabus+ course identifier
   * @param seriesId
   *          the matterhorn series identifier
   * @param name
   *          the course name
   * @param description
   *          the course description
   * @param externalCourseKey
   *          the external course key
   */
  public Course(String courseId, String seriesId, String name, String description, String externalCourseKey) {
    this.courseId = notEmpty(courseId, "courseId");
    this.seriesId = seriesId;
    this.name = name;
    this.description = description;
    this.externalCourseKey = externalCourseKey;
    this.fingerprint = Option.none(String.class);
    this.schedulingSource = Option.none(SchedulingSource.class);
  }

  /**
   * Sets the id
   *
   * @param id
   *          the course id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the course id
   *
   * @return the id
   */
  public Long getId() {
    return this.id;
  }

  public static final Function<Course, Option<Long>> getId = new Function<Course, Option<Long>>() {
    @Override
    public Option<Long> apply(Course course) {
      return option(course.getId());
    }
  };

  /**
   * Sets the syllabus course identifier
   *
   * @param courseId
   *          the syllabus course id
   */
  public void setCourseId(String courseId) {
    this.courseId = courseId;
  }

  /**
   * Returns the syllabus course identifier
   *
   * @return the syllabus course id
   */
  public String getCourseId() {
    return courseId;
  }

  /**
   * Sets the MH series identifier
   *
   * @param seriesId
   *          the series id
   */
  public void setSeriesId(String seriesId) {
    this.seriesId = seriesId;
  }

  /**
   * Returns the course name
   *
   * @return the course name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the course name
   *
   * @param name
   *          the course name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the course description
   *
   * @return the course description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the course description
   *
   * @param description
   *          the course description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Returns the external course key
   *
   * @return the external course key
   */
  public String getExternalCourseKey() {
    return externalCourseKey;
  }

  /**
   * Sets the external course key
   *
   * @param externalCourseKey
   *          the external course key
   */
  public void setExternalCourseKey(String externalCourseKey) {
    this.externalCourseKey = externalCourseKey;
  }

  /**
   * Returns the MH series identifier
   *
   * @return the series id
   */
  public String getSeriesId() {
    return seriesId;
  }

  /**
   * Sets the recording's fingerprint, which should not exceed 32 bits.
   *
   * @param fingerprint
   *          the fingerprint
   */
  public void setFingerprint(Option<String> fingerprint) {
    this.fingerprint = fingerprint;
  }

  /**
   * Returns the recording's fingerprint.
   *
   * @return the fingerprint
   */
  public Option<String> getFingerprint() {
    return fingerprint;
  }

  public void setSchedulingSource(Option<SchedulingSource> schedulingSource) {
    this.schedulingSource = schedulingSource;
  }

  public Option<SchedulingSource> getSchedulingSource() {
    return schedulingSource;
  }

  /**
   * Creates the series Id from the internal data
   *
   * @return the seriesId;
   */
  public String createSeriesId() {
    if (StringUtils.isBlank(externalCourseKey)) {
      return courseId;
    } else if (externalCourseKey.equals(courseId)) {
      return externalCourseKey; // don't encode further
    } else {
      // Manchester seriesId = md5(externalCourseKey)
      if (courseId.startsWith("V_")) {
        return "V_" + DigestUtils.md5Hex(externalCourseKey);
      } else {
        return DigestUtils.md5Hex(externalCourseKey);
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Course course = (Course) o;
    boolean checkSeries = seriesId == null ? course.getSeriesId() == null : seriesId.equals(course.getSeriesId());
    boolean checkName = name == null ? course.getName() == null : name.equals(course.getName());
    boolean checkDescription = description == null ? course.getDescription() == null : description.equals(course
            .getDescription());
    boolean checkSchedulingsource = schedulingSource.equals(course.getSchedulingSource());
    boolean checkExternalCourseKey = externalCourseKey == null ? course.getExternalCourseKey() == null
            : externalCourseKey.equals(course.getExternalCourseKey());
    return courseId.equals(course.getCourseId()) && checkSeries && checkName && checkDescription
            && checkSchedulingsource && checkExternalCourseKey;
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, courseId, seriesId);
  }

  @Override
  public String toString() {
    return "Course:" + courseId;
  }

  public Obj toJson() {
    return Jsons.obj(Jsons.p("name", name),
            Jsons.p("description", description));
  }
}
