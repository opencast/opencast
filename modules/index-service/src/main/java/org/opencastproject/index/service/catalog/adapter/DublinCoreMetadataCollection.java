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

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DublinCoreMetadataCollection extends AbstractMetadataCollection {
  private static final Logger logger = LoggerFactory.getLogger(DublinCoreMetadataCollection.class);

  private Opt<Boolean> getCollectionIsTranslatable(MetadataField<?> metadataField,
          ListProvidersService listProvidersService) {
    if (listProvidersService != null && metadataField.getListprovider().isSome()) {
      try {
        boolean isTranslatable = listProvidersService.isTranslatable(metadataField.getListprovider().get());
        return Opt.some(isTranslatable);
      } catch (ListProviderException ex) {
        // failed to get is-translatable property on list-provider-service
        // as this field is optional, it is fine to pass here
      }
    }
    return Opt.none();
  }

  @Override
  public MetadataCollection getCopy() {
    MetadataCollection copiedCollection = new DublinCoreMetadataCollection();
    for (MetadataField field : getFields()) {
      MetadataField copiedField = new MetadataField(field);
      copiedCollection.addField(copiedField);
    }
    return copiedCollection;
  }

  private Opt<Map<String, String>> getCollection(MetadataField<?> metadataField,
          ListProvidersService listProvidersService) {
    try {
      if (listProvidersService != null && metadataField.getListprovider().isSome()) {
        Map<String, String> collection = listProvidersService.getList(metadataField.getListprovider().get(),
                new ResourceListQueryImpl(), true);
        if (collection != null) {
          return Opt.some(collection);
        }
      }
      return Opt.none();
    } catch (ListProviderException e) {
      logger.warn("Unable to set collection on metadata because {}", ExceptionUtils.getStackTrace(e));
      return Opt.none();
    }
  }

  public void addEmptyField(MetadataField<?> metadataField, ListProvidersService listProvidersService) {
    addField(metadataField, Collections.emptyList(), listProvidersService);
  }

  public void addField(MetadataField<?> metadataField, String value, ListProvidersService listProvidersService) {
    addField(metadataField, Collections.singletonList(value), listProvidersService);
  }

  public void addField(MetadataField<?> metadataField, List<String> values, ListProvidersService listProvidersService) {

    List<String> filteredValues = values.stream()
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());

    if (!filteredValues.isEmpty()) {
      metadataField = MetadataField.setValueFromDCCatalog(filteredValues, metadataField);
    }

    metadataField.setIsTranslatable(getCollectionIsTranslatable(metadataField, listProvidersService));
    metadataField.setCollection(getCollection(metadataField, listProvidersService));

    addField(metadataField);
  }
}
