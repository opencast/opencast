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

package org.opencastproject.adminui.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;

import com.entwinemedia.fn.data.Opt;

import org.junit.Before;
import org.junit.Test;

public class DublinCoreMetadataCollectionTest {
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

  @Before
  public void setUp() {
    first = MetadataField.createTextMetadataField(FIRST_ID, null, FIRST_ID, false, false,
            null, null, null, 0,
            null);
    third = MetadataField.createTextMetadataField(THIRD_ID, null, THIRD_ID, false, false,
            null, null, null, 2,
            null);
    seventh = MetadataField.createTextMetadataField(SEVENTH_ID, null, SEVENTH_ID, false, false,
            null, null, null, 6,
            null);

    unorderedOne = MetadataField.createTextMetadataField(UNORDERED_ONE_ID, null, UNORDERED_ONE_ID,
            false, false, null, null, null,
            null, null);
    unorderedTwo = MetadataField.createTextMetadataField(UNORDERED_TWO_ID, null, UNORDERED_TWO_ID,
            false, false, null, null, null,
            null, null);
    unorderedThree = MetadataField.createTextMetadataField(UNORDERED_THREE_ID, null, UNORDERED_THREE_ID,
            false, false, null, null, null,
            null, null);
  }

  @Test
  public void testOrderOfFields() {
    final MetadataCollection collection = new DublinCoreMetadataCollection();
    // Add a single field that has an index greater than 0.
    collection.addField(third);
    assertEquals(1, collection.getFields().size());
    assertEquals(third, collection.getFields().get(0));
  }

  @Test
  public void testOrderOfFieldsInputFieldOrderZeroExpectsAtFront() {
    MetadataCollection collection = new DublinCoreMetadataCollection();
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);
    collection.addField(first);

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 0 should be first in the list of fields", first, collection
            .getFields().get(0));

    collection = new DublinCoreMetadataCollection();
    collection.addField(first);
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 0 should be first in the list of fields", first, collection
            .getFields().get(0));

    collection = new DublinCoreMetadataCollection();
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
    MetadataCollection collection = new DublinCoreMetadataCollection();
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);
    collection.addField(third);

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 2 should be in that position in the list of fields", third, collection
            .getFields().get(2));

    collection = new DublinCoreMetadataCollection();
    collection.addField(third);
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 2 should be in that position in the list of fields", third, collection
            .getFields().get(2));

    collection = new DublinCoreMetadataCollection();
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
    MetadataCollection collection = new DublinCoreMetadataCollection();
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

    collection = new DublinCoreMetadataCollection();
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

    collection = new DublinCoreMetadataCollection();
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
    final MetadataCollection collection = new DublinCoreMetadataCollection();
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);
    collection.addField(first);
    collection.addField(third);
    collection.addField(seventh);

    final MetadataField<String> newFirst = MetadataField.createTextMetadataField("New first", null,
            "New first", false, false, null, null, null,
            0, null);

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
    final MetadataCollection collection = new DublinCoreMetadataCollection();
    collection.addField(unorderedOne);
    collection.addField(unorderedTwo);
    collection.addField(unorderedThree);
    collection.addField(first);
    collection.addField(third);
    collection.addField(seventh);

    final MetadataField<String> newFirst = MetadataField.createTextMetadataField("first", null, "first",
            false, false, null, null, null, 0,
            null);
    final String value = "Hello";
    newFirst.setValue(value);
    collection.addField(newFirst);
    int numberOfFirsts = 0;
    Opt<String> valueFound = Opt.none();

    for (final MetadataField<?> field : collection.getFields()) {
      if (field.getInputID().equals(FIRST_ID)) {
        numberOfFirsts++;
        if (field.getValue() != null && field.getValue() instanceof String) {
          valueFound = Opt.some((String) field.getValue());
        }
      }
    }

    assertEquals("There should only be one field called first in the collection.", 1, numberOfFirsts);
    assertTrue("The value has been set so it should be in the collection.", valueFound.isSome());
    assertEquals("There should only be one field called first in the collection.", value, valueFound.get());
  }
}
