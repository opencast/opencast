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

package org.opencastproject.elasticsearch.index.series;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.opencastproject.util.DateTimeSupport;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SeriesTest {
  private static final Logger logger = LoggerFactory.getLogger(SeriesTest.class);
  private static final String ACCESS_POLICY_KEY = "access_policy";
  private static final String CONTRIBUTORS_JSON_KEY = "contributors";
  private static final String CONTRIBUTOR_JSON_KEY = "contributor";
  private static final String CREATED_DATE_TIME = "createdDateTime";
  private static final String CREATOR_JSON_KEY = "creator";
  private static final String DESCRIPTION_JSON_KEY = "description";
  private static final String IDENTIFIER_JSON_KEY = "identifier";
  private static final String LANGUAGE_JSON_KEY = "language";
  private static final String LICENSE_JSON_KEY = "license";
  private static final String ORGANIZATION_JSON_KEY = "organization";
  private static final String ORGANIZERS_JSON_KEY = "organizers";
  private static final String ORGANIZER_JSON_KEY = "organizer";
  private static final String SERIES_JSON_KEY = "series";
  private static final String SUBJECT_JSON_KEY = "subject";
  private static final String TITLE_JSON_KEY = "title";

  private String id = "10.0000-1";
  private String title = "Land and Vegetation: Key players on the Climate Scene";
  private String description = "This is the description for this series.";
  private String subject = "This is the subject";
  private String organization = "Organization";
  private String language = "Klingon";
  private String creator = "Creator Name";
  private String license = "Creative Commons 2.0";
  private String accessPolicy = "ROLE_ADMIN";
  private Date createdDateTime;
  private List<String> organizers = new ArrayList<String>();
  private String organizer1 = "organizer-one";
  private String organizer2 = "organizer-two";
  private String organizer3 = "organizer-three";

  private List<String> contributors = new ArrayList<String>();
  private String contributor1 = "contributor-one";
  private String contributor2 = "contributor-two";
  private String contributor3 = "contributor-three";

  private String seriesJson;
  private String seriesXml;

  @Before
  public void setUp() throws IOException, IllegalStateException, java.text.ParseException {
    createdDateTime = new Date(DateTimeSupport.fromUTC("2011-07-16T20:39:05Z"));
    // Setup presenter collection
    organizers.add(organizer1);
    organizers.add(organizer2);
    organizers.add(organizer3);
    // Setup contributors
    contributors.add(contributor1);
    contributors.add(contributor2);
    contributors.add(contributor3);
    // Setup results
    seriesJson = IOUtils.toString(getClass().getResource("/adminui_series_metadata.json"));
    seriesXml = IOUtils.toString(getClass().getResource("/adminui_series_metadata.xml"));
  }

  @Test
  public void testToJson() throws ParseException {
    // Initialize series
    Series series = new Series(id, organization);
    series.setTitle(title);
    series.setDescription(description);
    series.setSubject(subject);
    series.setLanguage(language);
    series.setCreator(creator);
    series.setLicense(license);
    series.setAccessPolicy(accessPolicy);
    series.setCreatedDateTime(createdDateTime);
    series.setOrganizers(organizers);
    series.setContributors(contributors);
    logger.info(series.toJSON());

    // Check that generated JSON has proper values
    JSONObject parse = (JSONObject) new JSONParser().parse(series.toJSON());
    if (parse.get(SERIES_JSON_KEY) == null || !(parse.get(SERIES_JSON_KEY) instanceof JSONObject)) {
      fail("There must be an series object returned.");
    }
    JSONObject seriesJsonObject = (JSONObject) parse.get(SERIES_JSON_KEY);
    assertEquals(id, seriesJsonObject.get(IDENTIFIER_JSON_KEY));
    assertEquals(title, seriesJsonObject.get(TITLE_JSON_KEY));
    assertEquals(description, seriesJsonObject.get(DESCRIPTION_JSON_KEY));
    assertEquals(subject, seriesJsonObject.get(SUBJECT_JSON_KEY));
    assertEquals(organization, seriesJsonObject.get(ORGANIZATION_JSON_KEY));
    assertEquals(language, seriesJsonObject.get(LANGUAGE_JSON_KEY));
    assertEquals(creator, seriesJsonObject.get(CREATOR_JSON_KEY));
    assertEquals(license, seriesJsonObject.get(LICENSE_JSON_KEY));
    assertEquals(accessPolicy, seriesJsonObject.get(ACCESS_POLICY_KEY));
    assertEquals(DateTimeSupport.toUTC(createdDateTime.getTime()), seriesJsonObject.get(CREATED_DATE_TIME));

    JSONObject organizersJsonObject = (JSONObject) seriesJsonObject.get(ORGANIZERS_JSON_KEY);
    if (organizersJsonObject == null || !(organizersJsonObject.get(ORGANIZER_JSON_KEY) instanceof JSONArray)) {
      fail("There should be an array of organizers returned.");
    }
    JSONArray organizersArray = (JSONArray) organizersJsonObject.get(ORGANIZER_JSON_KEY);
    // Ordering not important, just a convenient shorthand.
    assertEquals(organizer1, organizersArray.get(0));
    assertEquals(organizer2, organizersArray.get(1));
    assertEquals(organizer3, organizersArray.get(2));

    JSONObject contributorsJsonObject = (JSONObject) seriesJsonObject.get(CONTRIBUTORS_JSON_KEY);
    if (contributorsJsonObject == null || !(contributorsJsonObject.get(CONTRIBUTOR_JSON_KEY) instanceof JSONArray)) {
      fail("There should be an array of contributors returned.");
    }
    JSONArray contributorsArray = (JSONArray) contributorsJsonObject.get(CONTRIBUTOR_JSON_KEY);
    // Ordering not important, just a convenient shorthand.
    assertEquals(contributor1, contributorsArray.get(0));
    assertEquals(contributor2, contributorsArray.get(1));
    assertEquals(contributor3, contributorsArray.get(2));
  }
}
