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

package org.opencastproject.index.service.catalog.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.j;
import static com.entwinemedia.fn.data.json.Jsons.v;

import com.entwinemedia.fn.data.Opt;
import org.opencastproject.index.service.catalog.adapter.MetadataField.TYPE;
import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.security.api.Organization;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

public class MetadataFieldTest {
  private String defaultInputID = "TestInputID";
  private String defaultOutputID = "TestOutputID";
  private String startDate = "startDate";
  private String startTime = "startTime";
  private String endDate = "endDate";
  private String endTime = "endTime";
  private Opt<String> optOutputID = Opt.some(defaultOutputID);
  private String label = "A_LABEL_FOR_THIS_PROPERTY";
  private Map<String, Object> collection = new TreeMap<String, Object>();
  private Opt<Map<String, Object>> optCollection = Opt.some(collection);
  private String collectionID = "Collection_ID";
  private Opt<String> optCollectionID = Opt.some(collectionID);
  private boolean readOnly = false;
  private boolean required = false;
  private String datePattern = "yyyy-MM-dd";
  private String timePattern = "hh-mm-ss";
  private String dateTimePattern = datePattern + " " + timePattern;
  private String temporal = "start=2014-11-04T19:00:00Z; end=2014-11-05T20:00:00Z; scheme=W3C-DTF;";
  private ListProvidersService listProvidersService;
  private Date testDate = new Date(1415396970000L);

  @Before
  public void setUp() throws ListProviderException {
    collection.put("key-1", "value-1");
    collection.put("key-2", "value-2");
    collection.put("key-3", "value-3");

    listProvidersService = EasyMock.createMock(ListProvidersService.class);
    EasyMock.expect(
            listProvidersService.getList(EasyMock.anyString(), EasyMock.anyObject(ResourceListQuery.class),
                    EasyMock.anyObject(Organization.class))).andReturn(collection).anyTimes();
    EasyMock.replay(listProvidersService);
  }

  @Test
  public void testSetOutputIDInputNoOutputIDExpectsInputIDIsOutputID() {
    // If no outputID then getting the output id should return the input id.
    MetadataField<Date> dateField = MetadataField.createDateMetadata(defaultInputID, Opt.<String> none(), label,
            readOnly, required, datePattern, Opt.<Integer> none(), Opt.<String> none());
    assertEquals(defaultInputID, dateField.getOutputID());
  }

  @Test
  public void testSetOutputIDInputOutputIDExpectsOutputIDIsSet() {
    // If outputID is set then getting the output id should return the input id.
    MetadataField<Date> dateField = MetadataField.createDateMetadata(defaultInputID, optOutputID, label, readOnly,
            required, datePattern, Opt.<Integer> none(), Opt.<String> none());
    assertEquals(defaultOutputID, dateField.getOutputID());
  }

  @Test
  public void testCreateDateFieldJsonInputWithValueExpectsEmptyValueInJson() throws Exception {
    String dateJson = IOUtils.toString(getClass().getResource("/catalog-adapter/date/date-with-value.json"));
    MetadataField<Date> dateField = MetadataField.createDateMetadata(defaultInputID, Opt.<String> none(), label,
            readOnly, required, dateTimePattern, Opt.<Integer> none(), Opt.<String> none());
    dateField.setValue(testDate);
    assertThat(dateJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(dateField.toJSON())));
  }

  @Test
  public void testCreateDateFieldJsonInputWithBlankPatternExpectsEmptyValueInJson() throws Exception {
    MetadataField<Date> dateField = MetadataField.createDateMetadata(defaultInputID, Opt.<String> none(), label,
            readOnly, required, null, Opt.<Integer> none(), Opt.<String> none());
    dateField.setValue(testDate);

    SimpleDateFormat dateFormat = new SimpleDateFormat();
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    String expectedJSON = RestUtils.getJsonString(j(f("readOnly", v(readOnly)), f("id", v(defaultInputID)),
            f("label", v(label)), f("type", v(TYPE.DATE.toString().toLowerCase())),
            f("value", v(dateFormat.format(testDate))), f("required", v(required))));

    assertThat(expectedJSON, SameJSONAs.sameJSONAs(RestUtils.getJsonString(dateField.toJSON())));
  }

  @Test
  public void testCreateDateFieldJsonInputWithoutValueExpectsEmptyValueInJson() throws Exception {
    String dateJson = IOUtils.toString(getClass().getResource("/catalog-adapter/date/date-without-value.json"));
    MetadataField<Date> dateField = MetadataField.createDateMetadata(defaultInputID, Opt.<String> none(), label,
            readOnly, required, datePattern, Opt.<Integer> none(), Opt.<String> none());
    assertThat(dateJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(dateField.toJSON())));
  }

  @Test
  public void testCreateTextFieldJsonInputNoValueExpectsEmptyString() throws Exception {
    String emptyValueJson = IOUtils.toString(getClass().getResource("/catalog-adapter/text/text-empty-value.json"));
    // Test JSON generated with no value.
    MetadataField<String> emptyValueTextField = MetadataField.createTextMetadataField(defaultInputID, optOutputID,
            label, false, false, Opt.<Map<String, Object>> none(), Opt.<String> none(), Opt.<Integer> none(),
            Opt.<String> none());
    assertThat(emptyValueJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(emptyValueTextField.toJSON())));
  }

  @Test
  public void testCreateTextFieldJsonInputWithValueExpectsValue() throws Exception {
    String textValue = "This is the text value";
    String withValueJson = IOUtils.toString(getClass().getResource("/catalog-adapter/text/text-with-value.json"));
    // Test JSON with value
    MetadataField<String> textField = MetadataField.createTextMetadataField(defaultInputID, optOutputID, label, false,
            false, Opt.<Map<String, Object>> none(), Opt.<String> none(), Opt.<Integer> none(), Opt.<String> none());
    textField.setValue(textValue);
    assertThat(withValueJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(textField.toJSON())));
  }

  @Test
  public void testCreateTextFieldJsonInputWithCollectionExpectsCollectionPresentAndPopulated() throws Exception {
    // Test JSON with Collection
    String withCollectionJson = IOUtils.toString(getClass().getResource(
            "/catalog-adapter/text/text-with-collection.json"));
    MetadataField<String> textFieldWithCollection = MetadataField.createTextMetadataField(defaultInputID, optOutputID,
            label, false, false, optCollection, Opt.<String> none(), Opt.<Integer> none(), Opt.<String> none());
    assertThat(withCollectionJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(textFieldWithCollection.toJSON())));
  }

  @Test
  public void testCreateTextFieldJsonInputWithCollectionIDExpectsCollectionIDInCollectionProperty() throws Exception {
    // Test JSON with Collection ID
    String withCollectionIDJson = IOUtils.toString(getClass().getResource(
            "/catalog-adapter/text/text-with-collection-id.json"));
    MetadataField<String> textFieldWithCollectionID = MetadataField.createTextMetadataField(defaultInputID,
            optOutputID, label, false, false, Opt.<Map<String, Object>> none(), optCollectionID, Opt.<Integer> none(),
            Opt.<String> none());
    assertThat(withCollectionIDJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(textFieldWithCollectionID.toJSON())));
  }

  @Test
  public void testCreateTextLongFieldJsonInput() throws Exception {
    // Test JSON with Collection ID
    String withCollectionIDJson = IOUtils.toString(getClass().getResource(
            "/catalog-adapter/text/text-long-with-value.json"));
    MetadataField<String> textLongField = MetadataField.createTextLongMetadataField(defaultInputID, optOutputID, label,
            false, false, Opt.<Map<String, Object>> none(), Opt.<String> none(), Opt.<Integer> none(),
            Opt.<String> none());
    textLongField.setValue("This is the text value");
    assertThat(withCollectionIDJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(textLongField.toJSON())));
  }
}
