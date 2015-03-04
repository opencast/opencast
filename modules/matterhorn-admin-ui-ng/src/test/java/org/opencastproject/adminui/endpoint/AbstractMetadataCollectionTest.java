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
package org.opencastproject.adminui.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.index.service.catalog.adapter.AbstractMetadataCollection;
import org.opencastproject.index.service.catalog.adapter.MetadataField;

import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.JValue;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class AbstractMetadataCollectionTest {
  private static final String FIRST_ID = "first";
  private static final String THIRD_ID = "third";
  private static final String SEVENTH_ID = "seventh";
  private static final String UNORDERED_ONE_ID = "unordered-1";
  private static final String UNORDERED_TWO_ID = "unordered-2";
  private static final String UNORDERED_THREE_ID = "unordered-3";

  private MetadataField<String> first;
  private MetadataField<String> third;
  private MetadataField<String> seventh;
  private MetadataField<String> unorderedOne;
  private MetadataField<String> unorderedTwo;
  private MetadataField<String> unorderedThree;

  private AbstractMetadataCollection getAbstractMetadataCollection() {
    AbstractMetadataCollection collection = new AbstractMetadataCollection() {

      @Override
      public JValue toJSON() {
        return null;
      }
    };
    return collection;
  }

  @Before
  public void setUp() {
    first = MetadataField.createTextMetadataField(FIRST_ID, Opt.<String> none(), FIRST_ID, false, false,
            Opt.<Map<String, Object>> none(), Opt.<String> none(), Opt.some(0), Opt.<String> none());
    third = MetadataField.createTextMetadataField(THIRD_ID, Opt.<String> none(), THIRD_ID, false, false,
            Opt.<Map<String, Object>> none(), Opt.<String> none(), Opt.some(2), Opt.<String> none());
    seventh = MetadataField.createTextMetadataField(SEVENTH_ID, Opt.<String> none(), SEVENTH_ID, false, false,
            Opt.<Map<String, Object>> none(), Opt.<String> none(), Opt.some(6), Opt.<String> none());

    unorderedOne = MetadataField.createTextMetadataField(UNORDERED_ONE_ID, Opt.<String> none(), UNORDERED_ONE_ID,
            false, false, Opt.<Map<String, Object>> none(), Opt.<String> none(), Opt.<Integer> none(), Opt.<String> none());
    unorderedTwo = MetadataField.createTextMetadataField(UNORDERED_TWO_ID, Opt.<String> none(), UNORDERED_TWO_ID,
            false, false, Opt.<Map<String, Object>> none(), Opt.<String> none(), Opt.<Integer> none(), Opt.<String> none());
    unorderedThree = MetadataField.createTextMetadataField(UNORDERED_THREE_ID, Opt.<String> none(), UNORDERED_THREE_ID,
            false, false, Opt.<Map<String, Object>> none(), Opt.<String> none(), Opt.<Integer> none(), Opt.<String> none());
  }

  @Test
  public void testOrderOfFields() {
    AbstractMetadataCollection collection = getAbstractMetadataCollection();
    // Add a single field that has an index greater than 0.
    collection.addField(third);
    assertEquals(1, collection.getFields().size());
    assertEquals(third, collection.getFields().get(0));
  }

  @Test
  public void testOrderOfFieldsInputFieldOrderZeroExpectsAtFront() {
    AbstractMetadataCollection collection = getAbstractMetadataCollection();
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);
    collection.addField(first);

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 0 should be first in the list of fields", first, collection
            .getFields().get(0));

    collection = getAbstractMetadataCollection();
    collection.addField(first);
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 0 should be first in the list of fields", first, collection
            .getFields().get(0));

    collection = getAbstractMetadataCollection();
    collection.addField(unorderedOne);
    collection.addField(first);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 0 should be first in the list of fields", first, collection
            .getFields().get(0));
  }

  @Test
  public void testOrderOfFieldsInputFieldOrderTwoExpectsInMiddle() {
    AbstractMetadataCollection collection = getAbstractMetadataCollection();
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);
    collection.addField(third);

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 2 should be in that position in the list of fields", third, collection
            .getFields().get(2));

    collection = getAbstractMetadataCollection();
    collection.addField(third);
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 2 should be in that position in the list of fields", third, collection
            .getFields().get(2));

    collection = getAbstractMetadataCollection();
    collection.addField(unorderedOne);
    collection.addField(third);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 2 should be in that position in the list of fields", third, collection
            .getFields().get(2));
  }

  @Test
  public void testOrderOfFieldsInputMultipleOrderedFieldsExpectsInCorrectPositions() {
    AbstractMetadataCollection collection = getAbstractMetadataCollection();
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);
    collection.addField(first);
    collection.addField(third);
    collection.addField(seventh);

    assertEquals(6, collection.getFields().size());
    assertEquals("A field with an order value of 0 should be first in the list of fields", first, collection
            .getFields().get(0));
    assertEquals("A field with an order value of 2 should be in that position in the list of fields", third, collection
            .getFields().get(2));
    assertEquals("A field with an order value of 7 should be in the last position in the list of fields", seventh,
            collection.getFields().get(5));

    collection = getAbstractMetadataCollection();
    collection.addField(first);
    collection.addField(third);
    collection.addField(seventh);
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);

    assertEquals(6, collection.getFields().size());
    assertEquals("A field with an order value of 0 should be first in the list of fields", first, collection
            .getFields().get(0));
    assertEquals("A field with an order value of 2 should be in that position in the list of fields", third, collection
            .getFields().get(2));
    assertEquals("A field with an order value of 7 should be in the last position in the list of fields", seventh,
            collection.getFields().get(5));

    collection = getAbstractMetadataCollection();
    collection.addField(third);
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(first);
    collection.addField(seventh);
    collection.addField(unorderedThree);

    assertEquals(6, collection.getFields().size());
    assertEquals("A field with an order value of 0 should be first in the list of fields", first, collection
            .getFields().get(0));
    assertEquals("A field with an order value of 2 should be in that position in the list of fields", third, collection
            .getFields().get(2));
    assertEquals("A field with an order value of 7 should be in the last position in the list of fields", seventh,
            collection.getFields().get(5));
  }

  @Test
  public void testOrderOfFieldsInputDuplicateOrderValueExpectsBothInserted() {
    AbstractMetadataCollection collection = getAbstractMetadataCollection();
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);
    collection.addField(first);
    collection.addField(third);
    collection.addField(seventh);

    MetadataField<String> newFirst = MetadataField.createTextMetadataField("New first", Opt.<String> none(), "New first",
            false, false, Opt.<Map<String, Object>> none(), Opt.<String> none(), Opt.some(0), Opt.<String> none());

    collection.addField(newFirst);

    assertEquals(7, collection.getFields().size());
    assertTrue("A field with an order value of 0 should be first in the list of fields", first == collection
            .getFields().get(0) || newFirst == collection.getFields().get(0));
    assertTrue("A field with an order value of 0 should be second in the list of fields", first == collection
            .getFields().get(1) || newFirst == collection.getFields().get(1));
    assertEquals("A field with an order value of 2 should be in that position in the list of fields", third, collection
            .getFields().get(2));
    assertEquals("A field with an order value of 7 should be in the last position in the list of fields", seventh,
            collection.getFields().get(6));
  }

  @Test
  public void testAddExistingFieldInputAlreadyExistingFieldExpectsOnlyOneFieldFromGetFields() {
    AbstractMetadataCollection collection = getAbstractMetadataCollection();
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);
    collection.addField(first);
    collection.addField(third);
    collection.addField(seventh);

    MetadataField<String> newFirst = MetadataField.createTextMetadataField("first", Opt.<String> none(), "first",
            false, false, Opt.<Map<String, Object>> none(), Opt.<String> none(), Opt.some(0), Opt.<String> none());
    String value = "Hello";
    newFirst.setValue(value);
    collection.addField(newFirst);
    int numberOfFirsts = 0;
    Opt<String> valueFound = Opt.none();

    for (MetadataField<?> field : collection.getFields()) {
      if (field.getInputID().equals(FIRST_ID)) {
        numberOfFirsts++;
        if (field.getValue().isSome() && field.getValue().get() instanceof String) {
          valueFound = Opt.some((String)field.getValue().get());
        }
      }
    }

    assertEquals("There should only be one field called first in the collection.", 1, numberOfFirsts);
    assertTrue("The value has been set so it should be in the collection.", valueFound.isSome());
    assertEquals("There should only be one field called first in the collection.", value, valueFound.get());
  }
}
