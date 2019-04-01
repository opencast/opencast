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
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;

import com.entwinemedia.fn.data.Opt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ExternalMetadataUtils {
  private ExternalMetadataUtils() {
  }

  /**
   * Change a subject metadata field for a subjects json array.
   *
   * @param collection
   *          The collection to update subject to subjects.
   */
  public static void changeSubjectToSubjects(MetadataCollection collection) {
    // Change subject to subjects.
    MetadataField<?> subject = collection.getOutputFields().get(DublinCore.PROPERTY_SUBJECT.getLocalName());
    collection.removeField(subject);
    MetadataField<Iterable<String>> newSubjects = MetadataField.createIterableStringMetadataField(subject.getInputID(),
            Opt.some("subjects"), subject.getLabel(), subject.isReadOnly(), subject.isRequired(),
            subject.isTranslatable(), subject.getCollection(), subject.getCollectionID(), subject.getDelimiter(),
            subject.getOrder(), subject.getNamespace());
    List<String> subjectNames = new ArrayList<String>();
    if (subject.getValue().isSome()) {
      subjectNames = com.entwinemedia.fn.Stream.$(subject.getValue().get().toString().split(",")).toList();
    }
    newSubjects.setValue(subjectNames);
    collection.addField(newSubjects);
  }

  /**
   * Change the type of metadata fields from "ordered_text" to "text". This is necessary because "ordered_text" was
   * added later on, since it is needed by the admin UI. For the external API, backwards compatibility can be achieved
   * by just changing it back to the more general type "text". Unfortunately, meta data handling is shared between admin
   * UI and external api, which makes these conversions necessary.
   *
   * @param collection The collection to update.
   */
  public static void changeTypeOrderedTextToText(MetadataCollection collection) {
    for (final MetadataField<?> field : collection.getInputFields().values())  {
      if (MetadataField.Type.ORDERED_TEXT.equals(field.getType())) {
        field.setType(MetadataField.Type.TEXT);
      }
      if (MetadataField.JsonType.ORDERED_TEXT.equals(field.getJsonType())) {
        field.setJsonType(MetadataField.JsonType.TEXT);
      }
    }
  }

  /**
   * Remove the collection list from the metadata to reduce to amount of data
   *
   * @param metadata
   *          The metadata from which the list have to be removed
   */
  public static void removeCollectionList(MetadataCollection metadata) {
    // Change subject to subjects.
    List<MetadataField<?>> fields = metadata.getFields();
    for (MetadataField<?> f : fields) {
      f.setCollection(Opt.<Map<String, String>> none());
      f.setCollectionID(Opt.<String> none());
    }
  }
}
