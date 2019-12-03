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
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;

import com.entwinemedia.fn.data.Opt;
import com.google.common.collect.Iterables;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MetadataCollectionUtils {
  private static final Logger logger = LoggerFactory.getLogger(MetadataCollectionUtils.class);

  private MetadataCollectionUtils() {

  }

  private static Opt<Boolean> getCollectionIsTranslatable(
          final MetadataField<?> metadataField,
          final ListProvidersService listProvidersService) {
    if (listProvidersService != null && metadataField.getListprovider().isSome()) {
      try {
        final boolean isTranslatable = listProvidersService.isTranslatable(metadataField.getListprovider().get());
        return Opt.some(isTranslatable);
      } catch (final ListProviderException ex) {
        // failed to get is-translatable property on list-provider-service
        // as this field is optional, it is fine to pass here
      }
    }
    return Opt.none();
  }

  private static Opt<Map<String, String>> getCollection(
          final MetadataField<?> metadataField,
          final ListProvidersService listProvidersService) {
    try {
      if (listProvidersService != null && metadataField.getListprovider().isSome()) {
        final Map<String, String> collection = listProvidersService.getList(metadataField.getListprovider().get(),
                new ResourceListQueryImpl(), true);
        if (collection != null) {
          return Opt.some(collection);
        }
      }
      return Opt.none();
    } catch (final ListProviderException e) {
      logger.warn("Unable to set collection on metadata because", e);
      return Opt.none();
    }
  }

  public static void addEmptyField(final MetadataCollection collection, final MetadataField<?> metadataField, final ListProvidersService listProvidersService) {
    addField(collection, metadataField, Collections.emptyList(), listProvidersService);
  }

  public static void addField(final MetadataCollection collection, final MetadataField<?> metadataField, final String value, final ListProvidersService listProvidersService) {
    addField(collection, metadataField, Collections.singletonList(value), listProvidersService);
  }

  /**
   * Set value to a metadata field of unknown type
   */
  private static void setValueFromDCCatalog(
          final List<String> filteredValues,
          final MetadataField metadataField) {
    if (filteredValues.isEmpty()) {
      throw new IllegalArgumentException("Values cannot be empty");
    }

    if (filteredValues.size() > 1
            && metadataField.getType() != MetadataField.Type.MIXED_TEXT
            && metadataField.getType() != MetadataField.Type.ITERABLE_TEXT) {
      logger.warn("Cannot put multiple values into a single-value field, only the last value is used. {}",
              Arrays.toString(filteredValues.toArray()));
    }

    switch (metadataField.getType()) {
      case BOOLEAN:
        ((MetadataField<Boolean>)metadataField).setValue(Boolean.parseBoolean(Iterables.getLast(filteredValues)), false);
        break;
      case DATE:
        if (metadataField.getPattern().isNone()) {
          metadataField.setPattern(Opt.some("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        }
        ((MetadataField<Date>)metadataField).setValue(EncodingSchemeUtils.decodeDate(Iterables.getLast(filteredValues)), false);
        break;
      case DURATION:
        final String value = Iterables.getLast(filteredValues);
        final DCMIPeriod period = EncodingSchemeUtils.decodePeriod(value);
        final long longValue = period.getEnd().getTime() - period.getStart().getTime();
        ((MetadataField<String>)metadataField).setValue(Long.toString(longValue), false);
        break;
      case ITERABLE_TEXT:
      case MIXED_TEXT:
        ((MetadataField<Iterable<String>>)metadataField).setValue(filteredValues, false);
        break;
      case LONG:
        ((MetadataField<Long>)metadataField).setValue(Long.parseLong(Iterables.getLast(filteredValues)), false);
        break;
      case START_DATE:
        if (metadataField.getPattern().isNone()) {
          metadataField.setPattern(Opt.some("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
        }
        ((MetadataField<String>)metadataField).setValue(Iterables.getLast(filteredValues), false);
        break;
      case TEXT:
      case ORDERED_TEXT:
      case TEXT_LONG:
        ((MetadataField<String>)metadataField).setValue(Iterables.getLast(filteredValues), false);
        break;
      default:
        throw new IllegalArgumentException("Unknown metadata type! " + metadataField.getType());
    }
  }


  public static void addField(final MetadataCollection collection, final MetadataField<?> metadataField, final List<String> values, final ListProvidersService listProvidersService) {
    final List<String> filteredValues = values.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());

    if (!filteredValues.isEmpty()) {
      setValueFromDCCatalog(filteredValues, metadataField);
    }

    metadataField.setIsTranslatable(getCollectionIsTranslatable(metadataField, listProvidersService));
    metadataField.setCollection(getCollection(metadataField, listProvidersService));

    collection.addField(metadataField);
  }
}
