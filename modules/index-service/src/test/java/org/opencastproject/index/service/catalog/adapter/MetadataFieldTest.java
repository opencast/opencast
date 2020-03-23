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
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.metadata.dublincore.MetadataField;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import uk.co.datumedge.hamcrest.json.SameJSONAs;

public class MetadataFieldTest {
  private String defaultInputID = "TestInputID";
  private String defaultOutputID = "TestOutputID";
  private Opt<String> optOutputID = Opt.some(defaultOutputID);
  private String label = "A_LABEL_FOR_THIS_PROPERTY";
  private Map<String, String> collection = new TreeMap<>();
  private Opt<Map<String, String>> optCollection = Opt.some(collection);
  private String collectionID = "Collection_ID";
  private Opt<String> optCollectionID = Opt.some(collectionID);
  private boolean readOnly = false;
  private boolean required = false;
  private boolean isTranslatable = false;
  private String datePattern = "yyyy-MM-dd";
  private String timePattern = "hh-mm-ss";
  private String dateTimePattern = datePattern + " " + timePattern;
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
                    EasyMock.anyBoolean())).andReturn(collection).anyTimes();
    EasyMock.expect(
            listProvidersService.isTranslatable(EasyMock.anyString())).andReturn(isTranslatable).anyTimes();
    EasyMock.replay(listProvidersService);
  }

  @Test
  public void testSetDifferentValues()  throws Exception {
    String textValue = "This is the text value";

    MetadataField<String> textField = MetadataField.createTextMetadataField(defaultInputID, optOutputID, label, false,
            false, Opt.none(), Opt.none(), Opt.none(), Opt.none(), Opt.none());
    textField.setValue(textValue);

    assertTrue(textField.hasDifferentValues().isNone());

    textField.setDifferentValues();

    assertTrue(textField.hasDifferentValues().isSome());
    assertTrue(textField.hasDifferentValues().get());
    assertTrue(textField.getValue().isNone());

    String withDifferentValuesJson = IOUtils.toString(getClass()
            .getResource("/catalog-adapter/text/text-with-different-values.json"));
    assertThat(withDifferentValuesJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(textField.toJSON())));
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
    String expectedJSON = RestUtils.getJsonString(obj(f("readOnly", v(readOnly)), f("id", v(defaultInputID)),
            f("label", v(label)), f("type", v(MetadataField.Type.DATE.toString().toLowerCase())),
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
            label, false, false, Opt.<Boolean> none(), Opt.<Map<String, String>> none(), Opt.<String> none(),
            Opt.<Integer> none(), Opt.<String> none());
    assertThat(emptyValueJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(emptyValueTextField.toJSON())));
  }

  @Test
  public void testCreateTextFieldJsonInputWithValueExpectsValue() throws Exception {
    String textValue = "This is the text value";
    String withValueJson = IOUtils.toString(getClass().getResource("/catalog-adapter/text/text-with-value.json"));
    // Test JSON with value
    MetadataField<String> textField = MetadataField.createTextMetadataField(defaultInputID, optOutputID, label, false,
            false, Opt.<Boolean> none(), Opt.<Map<String, String>> none(), Opt.<String> none(), Opt.<Integer> none(),
            Opt.<String> none());
    textField.setValue(textValue);
    assertThat(withValueJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(textField.toJSON())));
  }

  @Test
  public void testCreateTextFieldJsonInputWithCollectionExpectsCollectionPresentAndPopulated() throws Exception {
    // Test JSON with Collection
    String withCollectionJson = IOUtils.toString(getClass().getResource(
            "/catalog-adapter/text/text-with-collection.json"));
    MetadataField<String> textFieldWithCollection = MetadataField.createTextMetadataField(defaultInputID, optOutputID,
            label, false, false, Opt.some(true), optCollection, Opt.<String> none(), Opt.<Integer> none(),
            Opt.<String> none());
    assertThat(withCollectionJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(textFieldWithCollection.toJSON())));
  }

  @Test
  public void testCreateTextFieldJsonInputWithCollectionIDExpectsCollectionIDInCollectionProperty() throws Exception {
    // Test JSON with Collection ID
    String withCollectionIDJson = IOUtils.toString(getClass().getResource(
            "/catalog-adapter/text/text-with-collection-id.json"));
    MetadataField<String> textFieldWithCollectionID = MetadataField.createTextMetadataField(defaultInputID,
            optOutputID, label, false, false, Opt.<Boolean> none(), Opt.<Map<String, String>> none(), optCollectionID,
            Opt.<Integer> none(), Opt.<String> none());
    assertThat(withCollectionIDJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(textFieldWithCollectionID.toJSON())));
  }

  @Test
  public void testCreateTextLongFieldJsonInput() throws Exception {
    // Test JSON with Collection ID
    String withCollectionIDJson = IOUtils.toString(getClass().getResource(
            "/catalog-adapter/text/text-long-with-value.json"));
    MetadataField<String> textLongField = MetadataField.createTextLongMetadataField(defaultInputID, optOutputID, label,
            false, false, Opt.<Boolean> none(),Opt.<Map<String, String>> none(), Opt.<String> none(),
            Opt.<Integer> none(), Opt.<String> none());
    textLongField.setValue("This is the text value");
    assertThat(withCollectionIDJson, SameJSONAs.sameJSONAs(RestUtils.getJsonString(textLongField.toJSON())));
  }
}
