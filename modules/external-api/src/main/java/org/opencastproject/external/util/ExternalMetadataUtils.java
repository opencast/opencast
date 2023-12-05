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
package org.opencastproject.external.util;

import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ExternalMetadataUtils {
  private ExternalMetadataUtils() {
  }

  /**
   * Change a subject metadata field for a subjects json array.
   *
   * @param collection
   *          The collection to update subject to subjects.
   */
  public static void changeSubjectToSubjects(final DublinCoreMetadataCollection collection) {
    // Change subject to subjects.
    final MetadataField subject = collection.getOutputFields().get(DublinCore.PROPERTY_SUBJECT.getLocalName());
    collection.removeField(subject);
    final List<String> newValue;
    if (subject.getValue() != null) {
      newValue = Pattern.compile(",").splitAsStream(subject.getValue().toString()).collect(Collectors.toList());
    } else {
      newValue = null;
    }
    collection.addField(new MetadataField(
            subject.getInputID(),
            "subjects",
            subject.getLabel(),
            subject.isReadOnly(),
            subject.isRequired(),
            newValue,
            subject.isTranslatable(),
            MetadataField.Type.ITERABLE_TEXT,
            subject.getCollection(),
            subject.getCollectionID(),
            subject.getOrder(),
            subject.getNamespace(),
            subject.getListprovider(),
            subject.getPattern(),
            subject.getDelimiter()));
  }

  /**
   * Remove the collection list from the metadata to reduce to amount of data
   *
   * @param metadata
   *          The metadata from which the list have to be removed
   */
  public static void removeCollectionList(final DublinCoreMetadataCollection metadata) {
    // Change subject to subjects.
    final List<MetadataField> fields = metadata.getFields();
    for (final MetadataField f : fields) {
      f.setCollection(null);
      f.setCollectionID(null);
    }
  }
}
