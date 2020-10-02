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

import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.list.api.ListProviderException;
import org.opencastproject.list.api.ListProvidersService;
import org.opencastproject.list.api.ResourceListQuery;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.metadata.dublincore.MetadataJson;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

public class MetadataFieldTest {
  private final String defaultInputID = "TestInputID";
  private final String defaultOutputID = "TestOutputID";
  private final String optOutputID = defaultOutputID;
  private final String label = "A_LABEL_FOR_THIS_PROPERTY";
  private final Map<String, String> collection = new TreeMap<>();
  private final Map<String, String> optCollection = collection;
  private final String collectionID = "Collection_ID";
  private final String optCollectionID = collectionID;
  private final boolean readOnly = false;
  private final boolean required = false;
  private final boolean isTranslatable = false;
  private final String datePattern = "yyyy-MM-dd";
  private final String timePattern = "hh-mm-ss";
  private final String dateTimePattern = datePattern + " " + timePattern;
  private ListProvidersService listProvidersService;
  private final Date testDate = new Date(1415396970000L);

  @Before
  public void setUp() throws ListProviderException {
    collection.put("key-1", "value-1");
    collection.put("key-2", "value-2");
    collection.put("key-3", "value-3");

    listProvidersService = EasyMock.createMock(ListProvidersService.class);
    EasyMock.expect(
            listProvidersService.getList(EasyMock.anyString(), EasyMock.anyObject(ResourceListQuery.class),
                    EasyMock.anyBoolean())).andReturn(collection).anyTimes();
    EasyMock.expect(
            listProvidersService.isTranslatable(EasyMock.anyString())).andReturn(isTranslatable).anyTimes();
    EasyMock.replay(listProvidersService);
  }

  @Test
  public void testSetDifferentValues()  throws Exception {
    String textValue = "This is the text value";

    MetadataField textField = new MetadataField(
            defaultInputID,
            optOutputID,
            label,
            false,
            false,
            null,
            null,
            MetadataField.Type.TEXT,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    textField.setValue(textValue);

    assertNull(textField.hasDifferentValues());

    textField.setDifferentValues();

    assertNotNull(textField.hasDifferentValues());
    assertTrue(textField.hasDifferentValues());
    assertNull(textField.getValue());

    String withDifferentValuesJson = IOUtils.toString(getClass()
            .getResource("/catalog-adapter/text/text-with-different-values.json"), StandardCharsets.UTF_8);
    assertThat(
            withDifferentValuesJson,
            SameJSONAs.sameJSONAs(RestUtils.getJsonString(MetadataJson.fieldToJson(textField, true))));
  }


  @Test
  public void testSetOutputIDInputNoOutputIDExpectsInputIDIsOutputID() {
    // If no outputID then getting the output id should return the input id.

    final MetadataField dateField1 = new MetadataField(
            defaultInputID,
            null,
            label,
            readOnly,
            required,
            null,
            null,
            MetadataField.Type.DATE,
            null,
            null,
            null,
            null,
            null,
            StringUtils.isNotBlank(datePattern) ? datePattern : null,
            null);
    assertEquals(defaultInputID, dateField1.getOutputID());
  }

  @Test
  public void testSetOutputIDInputOutputIDExpectsOutputIDIsSet() {
    // If outputID is set then getting the output id should return the input id.

    final MetadataField dateField1 = new MetadataField(
            defaultInputID,
            optOutputID,
            label,
            readOnly,
            required,
            null,
            null,
            MetadataField.Type.DATE,
            null,
            null,
            null,
            null,
            null,
            StringUtils.isNotBlank(datePattern) ? datePattern : null,
            null);
    assertEquals(defaultOutputID, dateField1.getOutputID());
  }

  @Test
  public void testCreateDateFieldJsonInputWithValueExpectsEmptyValueInJson() throws Exception {
    final String dateJson = IOUtils.toString(getClass().getResource("/catalog-adapter/date/date-with-value.json"),
            StandardCharsets.UTF_8);

    final MetadataField dateField1 = new MetadataField(
            defaultInputID,
            null,
            label,
            readOnly,
            required,
            null,
            null,
            MetadataField.Type.DATE,
            null,
            null,
            null,
            null,
            null,
            StringUtils.isNotBlank(dateTimePattern) ? dateTimePattern : null,
            null);
    dateField1.setValue(testDate);
    assertThat(dateJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(MetadataJson.fieldToJson(dateField1, true))));
  }

  @Test
  public void testCreateDateFieldJsonInputWithBlankPatternExpectsNonEmptyValueInJson() throws Exception {

    final MetadataField dateField1 = new MetadataField(
            defaultInputID,
            null,
            label,
            readOnly,
            required,
            null,
            null,
            MetadataField.Type.DATE,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    dateField1.setValue(testDate);

    final SimpleDateFormat dateFormat = new SimpleDateFormat();
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    final String expectedJSON = RestUtils.getJsonString(obj(f("readOnly", v(readOnly)), f("id", v(defaultInputID)),
            f("label", v(label)), f("type", v(MetadataField.Type.DATE.toString().toLowerCase())),
            f("value", v(dateFormat.format(testDate))), f("required", v(required))));

    assertThat(expectedJSON, SameJSONAs.sameJSONAs(RestUtils.getJsonString(MetadataJson.fieldToJson(dateField1, true))));
  }

  @Test
  public void testCreateDateFieldJsonInputWithoutValueExpectsEmptyValueInJson() throws Exception {
    final String dateJson = IOUtils
            .toString(getClass().getResource("/catalog-adapter/date/date-without-value.json"), StandardCharsets.UTF_8);

    final MetadataField dateField1 = new MetadataField(
            defaultInputID,
            null,
            label,
            readOnly,
            required,
            null,
            null,
            MetadataField.Type.DATE,
            null,
            null,
            null,
            null,
            null,
            StringUtils.isNotBlank(datePattern) ? datePattern : null,
            null);
    assertThat(dateJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(MetadataJson.fieldToJson(dateField1, true))));
  }

  @Test
  public void testCreateTextFieldJsonInputNoValueExpectsEmptyString() throws Exception {
    final String emptyValueJson = IOUtils.toString(getClass().getResource("/catalog-adapter/text/text-empty-value.json"), StandardCharsets.UTF_8);
    // Test JSON generated with no value.
    final MetadataField emptyValueTextField = new MetadataField(
            defaultInputID,
            optOutputID,
            label,
            false,
            false,
            null,
            null,
            MetadataField.Type.TEXT,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    assertThat(emptyValueJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(MetadataJson.fieldToJson(emptyValueTextField,
            true))));
  }

  @Test
  public void testCreateTextFieldJsonInputWithValueExpectsValue() throws Exception {
    final String textValue = "This is the text value";
    final String withValueJson = IOUtils
            .toString(getClass().getResource("/catalog-adapter/text/text-with-value.json"), StandardCharsets.UTF_8);
    // Test JSON with value
    final MetadataField textField = new MetadataField(
            defaultInputID,
            optOutputID,
            label,
            false,
            false,
            null,
            null,
            MetadataField.Type.TEXT,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    textField.setValue(textValue);
    assertThat(withValueJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(MetadataJson.fieldToJson(textField, true))));
  }

  @Test
  public void testCreateTextFieldJsonInputWithCollectionExpectsCollectionPresentAndPopulated() throws Exception {
    // Test JSON with Collection
    final String withCollectionJson = IOUtils.toString(getClass().getResource(
            "/catalog-adapter/text/text-with-collection.json"), StandardCharsets.UTF_8);
    final MetadataField textFieldWithCollection = new MetadataField(
            defaultInputID,
            optOutputID,
            label,
            false,
            false,
            "",
            true,
            MetadataField.Type.TEXT,
            optCollection,
            null,
            null,
            null,
            null,
            null,
            null);
    assertThat(
            withCollectionJson,
            SameJSONAs.sameJSONAs(RestUtils.getJsonString(MetadataJson.fieldToJson(textFieldWithCollection, true))));
  }

  @Test
  public void testCreateTextFieldJsonInputWithCollectionIDExpectsCollectionIDInCollectionProperty() throws Exception {
    // Test JSON with Collection ID
    final String withCollectionIDJson = IOUtils.toString(getClass().getResource(
            "/catalog-adapter/text/text-with-collection-id.json"), StandardCharsets.UTF_8);
    final MetadataField textFieldWithCollectionID = new MetadataField(
            defaultInputID,
            optOutputID,
            label,
            false,
            false,
            null,
            null,
            MetadataField.Type.TEXT,
            null,
            optCollectionID,
            null,
            null,
            null,
            null,
            null);
    assertThat(
            withCollectionIDJson,
            SameJSONAs.sameJSONAs(RestUtils.getJsonString(MetadataJson.fieldToJson(textFieldWithCollectionID, true))));
  }

  @Test
  public void testCreateTextLongFieldJsonInput() throws Exception {
    // Test JSON with Collection ID
    final String withCollectionIDJson = IOUtils.toString(getClass().getResource(
            "/catalog-adapter/text/text-long-with-value.json"), StandardCharsets.UTF_8);
    final MetadataField textLongField = new MetadataField(
            defaultInputID,
            optOutputID,
            label,
            false,
            false,
            null,
            null,
            MetadataField.Type.TEXT_LONG,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    textLongField.setValue("This is the text value");
    assertThat(
            withCollectionIDJson,
            SameJSONAs.sameJSONAs(RestUtils.getJsonString(MetadataJson.fieldToJson(textLongField, true))));
  }
}
