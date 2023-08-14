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

package org.opencastproject.metadata.dublincore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.entwinemedia.fn.data.Opt;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class DublinCoreMetadataCollectionTest {
  private static final String FIRST_ID = "first";
  private static final String THIRD_ID = "third";
  private static final String SEVENTH_ID = "seventh";
  private static final String UNORDERED_ONE_ID = "unordered-1";
  private static final String UNORDERED_TWO_ID = "unordered-2";
  private static final String UNORDERED_THREE_ID = "unordered-3";

  private MetadataField first;
  private MetadataField third;
  private MetadataField seventh;
  private MetadataField unorderedOne;
  private MetadataField unorderedTwo;
  private MetadataField unorderedThree;

  private static MetadataField createField(final String label, final Integer order, final String value) {
    return new MetadataField(
      label,
      null,
      label,
      false,
      false,
      value,
      null,
      MetadataField.Type.TEXT,
      null,
      null,
      order,
      null,
      null,
      null,
      null);
  }

  @Before
  public void setUp() {
    first = createField(FIRST_ID, 0, null);
    third = createField(THIRD_ID, 2, null);
    seventh = createField(SEVENTH_ID, 6, null);

    unorderedOne = createField(UNORDERED_ONE_ID, null, null);
    unorderedTwo = createField(UNORDERED_TWO_ID, null, null);
    unorderedThree = createField(UNORDERED_THREE_ID, null, null);
  }

  @Test
  public void testOrderOfFields() {
    // Add a single field that has an index greater than 0.
    final DublinCoreMetadataCollection collection = new DublinCoreMetadataCollection(Collections.singleton(third));
    assertEquals(1, collection.getFields().size());
    assertEquals(third, collection.getFields().get(0));
  }

  @Test
  public void testOrderOfFieldsInputFieldOrderZeroExpectsAtFront() {
    DublinCoreMetadataCollection collection = new DublinCoreMetadataCollection(Arrays
      .asList(unorderedOne, unorderedTwo, unorderedThree, first));

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 0 should be first in the list of fields", first, collection
            .getFields().get(0));

    collection = new DublinCoreMetadataCollection(Arrays.asList(first, unorderedOne, unorderedTwo, unorderedThree));

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 0 should be first in the list of fields", first, collection
            .getFields().get(0));

    collection = new DublinCoreMetadataCollection(Arrays.asList(unorderedOne, first, unorderedTwo, unorderedThree));

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 0 should be first in the list of fields", first, collection
            .getFields().get(0));
  }

  @Test
  public void testOrderOfFieldsInputFieldOrderTwoExpectsInMiddle() {
    DublinCoreMetadataCollection collection = new DublinCoreMetadataCollection(Arrays
      .asList(unorderedOne, unorderedTwo, unorderedThree, third));

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 2 should be in that position in the list of fields", third, collection
            .getFields().get(2));

    collection = new DublinCoreMetadataCollection(Arrays.asList(third, unorderedOne, unorderedTwo, unorderedThree));

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 2 should be in that position in the list of fields", third, collection
            .getFields().get(2));

    collection = new DublinCoreMetadataCollection(Arrays.asList(unorderedOne, third, unorderedTwo, unorderedThree));

    assertEquals(4, collection.getFields().size());
    assertEquals("A field with an order value of 2 should be in that position in the list of fields", third, collection
            .getFields().get(2));
  }

  @Test
  public void testOrderOfFieldsInputMultipleOrderedFieldsExpectsInCorrectPositions() {
    DublinCoreMetadataCollection collection = new DublinCoreMetadataCollection(Arrays
      .asList(unorderedOne, unorderedTwo, unorderedThree, first, third, seventh));

    assertEquals(6, collection.getFields().size());
    assertEquals("A field with an order value of 0 should be first in the list of fields", first, collection
            .getFields().get(0));
    assertEquals("A field with an order value of 2 should be in that position in the list of fields", third, collection
            .getFields().get(2));
    assertEquals("A field with an order value of 7 should be in the last position in the list of fields", seventh,
            collection.getFields().get(5));

    collection = new DublinCoreMetadataCollection(Arrays
      .asList(first, third, seventh, unorderedOne, unorderedTwo, unorderedThree));

    assertEquals(6, collection.getFields().size());
    assertEquals("A field with an order value of 0 should be first in the list of fields", first, collection
            .getFields().get(0));
    assertEquals("A field with an order value of 2 should be in that position in the list of fields", third, collection
            .getFields().get(2));
    assertEquals("A field with an order value of 7 should be in the last position in the list of fields", seventh,
            collection.getFields().get(5));

    collection = new DublinCoreMetadataCollection(Arrays
      .asList(third, unorderedOne, unorderedTwo, first, seventh, unorderedThree));

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
    final MetadataField newFirst = createField("New first", 0, null);
    final DublinCoreMetadataCollection collection = new DublinCoreMetadataCollection(Arrays
      .asList(unorderedOne, unorderedTwo, unorderedThree, first, third, seventh, newFirst));

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
    final String value = "Hello";
    final MetadataField newFirst = createField("first", 0, value);

    final DublinCoreMetadataCollection collection = new DublinCoreMetadataCollection(Arrays
      .asList(unorderedOne, unorderedTwo, unorderedThree, first, third, seventh, newFirst));

    int numberOfFirsts = 0;
    Opt<String> valueFound = Opt.none();

    for (final MetadataField field : collection.getFields()) {
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
