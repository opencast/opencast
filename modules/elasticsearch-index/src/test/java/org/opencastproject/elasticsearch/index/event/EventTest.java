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

package org.opencastproject.elasticsearch.index.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.opencastproject.scheduler.api.RecordingState;
import org.opencastproject.security.api.DefaultOrganization;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

public class EventTest {
  private static final Logger logger = LoggerFactory.getLogger(EventTest.class);

  private static final String ENTRY_KEY = "entry";
  private static final String LOCATION_JSON_KEY = "location";
  private static final String DESCRIPTION_JSON_KEY = "description";
  private static final String CONTRIBUTOR_JSON_KEY = "contributor";
  private static final String CONTRIBUTORS_JSON_KEY = "contributors";
  private static final Object PRESENTER_JSON_KEY = "presenter";
  private static final String PRESENTERS_JSON_KEY = "presenters";
  private static final String SUBJECT_JSON_KEY = "subject";
  private static final String TITLE_JSON_KEY = "title";
  private static final String ORGANIZATION_JSON_KEY = "organization";
  private static final String IDENTIFIER_JSON_KEY = "identifier";
  private static final String EVENT_JSON_KEY = "event";
  private static final String AGENT_CONFIGURATION_KEY = "agent_configuration";

  private String id = "10.0000-1";
  private String title = "Land and Vegetation: Key players on the Climate Scene";
  private String description = "This is the description for this event.";
  private String subject = "This is the subject";
  private String location = "arts143ca";

  private List<String> presenters = new ArrayList<String>();
  private String presenter1 = "presenter-one";
  private String presenter2 = "presenter-two";
  private String presenter3 = "presenter-three";

  private List<String> contributors = new ArrayList<String>();
  private String contributor1 = "contributor-one";
  private String contributor2 = "contributor-two";
  private String contributor3 = "contributor-three";

  private Map<String, String> agentConfiguration = new HashMap<String, String>();
  private String agentConfigurationKey1 = "key-1";
  private String agentConfigurationValue1 = "value-1";
  private String agentConfigurationKey2 = "key-2";
  private String agentConfigurationValue2 = "value-2";
  private String agentConfigurationKey3 = "key-3";
  private String agentConfigurationValue3 = "value-3";

  private String eventJson;
  private String eventXml;
  private String eventCAConfigJson;

  private final String defaultOrganization = new DefaultOrganization().getId();

  @Before
  public void setUp() throws IOException {
    // Setup presenter collection
    presenters.add(presenter1);
    presenters.add(presenter2);
    presenters.add(presenter3);
    // Setup contributors
    contributors.add(contributor1);
    contributors.add(contributor2);
    contributors.add(contributor3);
    // Setup agent configurations
    agentConfiguration.put(agentConfigurationKey1, agentConfigurationValue1);
    agentConfiguration.put(agentConfigurationKey2, agentConfigurationValue2);
    agentConfiguration.put(agentConfigurationKey3, agentConfigurationValue3);
    // Setup results
    eventJson = IOUtils.toString(getClass().getResource("/adminui_event_metadata.json"));
    eventXml = IOUtils.toString(getClass().getResource("/adminui_event_metadata.xml"));
    eventCAConfigJson = IOUtils.toString(getClass().getResource("/adminui_event_metadata_agent_configuration.json"));
  }

  @Test
  public void testToJson() throws ParseException, IOException {
    Event event = new Event(id, defaultOrganization);
    event.setTitle(title);
    event.setDescription(description);
    event.setSubject(subject);
    event.setLocation(location);
    event.setPresenters(presenters);
    event.setContributors(contributors);
    event.setAgentConfiguration(agentConfiguration);
    logger.info(event.toJSON());
    JSONObject parse = (JSONObject) new JSONParser().parse(event.toJSON());
    if (parse.get(EVENT_JSON_KEY) == null || !(parse.get(EVENT_JSON_KEY) instanceof JSONObject)) {
      fail("There must be an event object returned.");
    }
    JSONObject eventJsonObject = (JSONObject) parse.get(EVENT_JSON_KEY);
    assertEquals(id, eventJsonObject.get(IDENTIFIER_JSON_KEY));
    assertEquals(defaultOrganization, eventJsonObject.get(ORGANIZATION_JSON_KEY));
    assertEquals(title, eventJsonObject.get(TITLE_JSON_KEY));
    assertEquals(description, eventJsonObject.get(DESCRIPTION_JSON_KEY));
    assertEquals(subject, eventJsonObject.get(SUBJECT_JSON_KEY));
    assertEquals(location, eventJsonObject.get(LOCATION_JSON_KEY));

    JSONArray presentersArray = (JSONArray) ((JSONObject) eventJsonObject.get(PRESENTERS_JSON_KEY))
            .get(PRESENTER_JSON_KEY);
    // Ordering not important, just a convenient shorthand.
    assertEquals(presenter1, presentersArray.get(0));
    assertEquals(presenter2, presentersArray.get(1));
    assertEquals(presenter3, presentersArray.get(2));

    JSONArray contributorsArray = (JSONArray) ((JSONObject) eventJsonObject.get(CONTRIBUTORS_JSON_KEY))
            .get(CONTRIBUTOR_JSON_KEY);
    // Ordering not important, just a convenient shorthand.
    assertEquals(contributor1, contributorsArray.get(0));
    assertEquals(contributor2, contributorsArray.get(1));
    assertEquals(contributor3, contributorsArray.get(2));

    JSONObject agentConfigurationObject = (JSONObject) eventJsonObject.get(AGENT_CONFIGURATION_KEY);
    JSONArray entryArray = (JSONArray) agentConfigurationObject.get(ENTRY_KEY);
    // Ordering not important
    assertThat(eventCAConfigJson, SameJSONAs.sameJSONAs(entryArray.toJSONString()).allowingAnyArrayOrdering());
  }

  @Test
  public void testHasRecordingStarted() {
    Event event = new Event(id, defaultOrganization);

    // This is not a scheduled event so the recording cannot have started
    assertFalse(event.hasRecordingStarted());

    event.setAgentId("test");

    // With a valid capture agent ID, it is a scheduled event, but the recording cannot have started
    // as we don't have a start time
    assertFalse(event.hasRecordingStarted());

    event.setRecordingStatus(RecordingState.CAPTURING);

    // The event is a scheduled event and has a valid recording state, so the recording has started
    assertTrue(event.hasRecordingStarted());
  }

}
