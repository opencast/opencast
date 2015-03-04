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
package org.opencastproject.index.service.impl.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.security.api.DefaultOrganization;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

public class EventTest {

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

  private String eventJson;
  private String eventXml;

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
    // Setup results
    eventJson = IOUtils.toString(getClass().getResource("/adminui_event_metadata.json"));
    eventXml = IOUtils.toString(getClass().getResource("/adminui_event_metadata.xml"));
  }

  @Ignore
  @Test
  public void testValueOf() throws ParseException, IOException, JSONException, XMLStreamException, JAXBException {
    Event event = Event.valueOf(IOUtils.toInputStream(eventXml));
    assertEquals(id, event.getIdentifier());
    assertEquals(title, event.getTitle());
    assertEquals(description, event.getDescription());
    assertEquals(subject, event.getSubject());
    assertEquals(location, event.getLocation());
    assertEquals(presenter1, event.getPresenters().get(0));
    assertEquals(presenter2, event.getPresenters().get(1));
    assertEquals(presenter3, event.getPresenters().get(2));
    assertEquals(contributor1, event.getContributors().get(0));
    assertEquals(contributor2, event.getContributors().get(1));
    assertEquals(contributor3, event.getContributors().get(2));
  }

  @Ignore
  @Test
  public void testValueOfJson() throws ParseException, IOException, JSONException, XMLStreamException, JAXBException {
    Event event = Event.valueOfJson(IOUtils.toInputStream(eventJson));
    assertEquals(id, event.getIdentifier());
    assertEquals(title, event.getTitle());
    assertEquals(presenter1, event.getPresenters().get(0));
    assertEquals(presenter2, event.getPresenters().get(1));
    assertEquals(presenter3, event.getPresenters().get(2));
    assertEquals(contributor1, event.getContributors().get(0));
    assertEquals(contributor2, event.getContributors().get(1));
    assertEquals(contributor3, event.getContributors().get(2));
  }

  @Test
  public void testToJson() throws ParseException {
    Event event = new Event(id, defaultOrganization);
    event.setTitle(title);
    event.setDescription(description);
    event.setSubject(subject);
    event.setLocation(location);
    event.setPresenters(presenters);
    event.setContributors(contributors);
    System.out.println(event.toJSON());
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
  }
}
