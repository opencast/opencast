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
package org.opencastproject.index.service.catalog.adapter;

import org.opencastproject.index.service.exception.ListProviderException;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DublinCoreMetadataCollection extends AbstractMetadataCollection {
  private static final Logger logger = LoggerFactory.getLogger(DublinCoreMetadataCollection.class);

  private Opt<Map<String, Object>> getCollection(MetadataField<?> metadataField,
          ListProvidersService listProvidersService) {
    try {
      if (listProvidersService != null && metadataField.getListprovider().isSome()) {
        Map<String, Object> collection = listProvidersService.getList(metadataField.getListprovider().get(),
                new ResourceListQueryImpl(), null);
        if (collection != null) {
          return Opt.some(collection);
        } else {
          return Opt.<Map<String, Object>> none();
        }
      } else {
        return Opt.<Map<String, Object>> none();
      }
    } catch (ListProviderException e) {
      logger.warn("Unable to set collection on metadata because {}", ExceptionUtils.getStackTrace(e));
      return Opt.<Map<String, Object>> none();
    }
  }

  public void addField(MetadataField<?> metadataField, String value, ListProvidersService listProvidersService) {
    switch (metadataField.getType()) {
      case BOOLEAN:
        MetadataField<Boolean> booleanField = MetadataField.createBooleanMetadata(metadataField.getInputID(),
                Opt.some(metadataField.getOutputID()), metadataField.getLabel(), metadataField.isReadOnly(),
                metadataField.isRequired(), metadataField.getOrder(), metadataField.getNamespace());
        if (StringUtils.isNotBlank(value)) {
          booleanField.setValue(Boolean.parseBoolean(value));
        }
        addField(booleanField);
        break;
      case DATE:
        MetadataField<Date> dateField = MetadataField.createDateMetadata(metadataField.getInputID(),
                Opt.some(metadataField.getOutputID()), metadataField.getLabel(), metadataField.isReadOnly(),
                metadataField.isRequired(), metadataField.getPattern().get(), metadataField.getOrder(),
                metadataField.getNamespace());
        if (StringUtils.isNotBlank(value)) {
          dateField.setValue(EncodingSchemeUtils.decodeDate(value));
        }
        addField(dateField);
        break;
      case DURATION:
        MetadataField<String> durationField = MetadataField.createDurationMetadataField(metadataField.getInputID(),
                Opt.some(metadataField.getOutputID()), metadataField.getLabel(), metadataField.isReadOnly(),
                metadataField.isRequired(), getCollection(metadataField, listProvidersService),
                metadataField.getCollectionID(), metadataField.getOrder(), metadataField.getNamespace());

        DCMIPeriod period = EncodingSchemeUtils.decodePeriod(value);
        Long longValue = -1L;

        // Check to see if it is from the front end
        String[] durationParts = value.split(":");
        if (durationParts.length == 3) {
          Integer hours = Integer.parseInt(durationParts[0]);
          Integer minutes = Integer.parseInt(durationParts[1]);
          Integer seconds = Integer.parseInt(durationParts[2]);
          longValue = ((hours.longValue() * 60 + minutes.longValue()) * 60 + seconds.longValue()) * 1000;
        } else if (period != null && period.hasStart() && period.hasEnd()) {
          longValue = period.getEnd().getTime() - period.getStart().getTime();
        } else {
          try {
            longValue = Long.parseLong(value);
          } catch (NumberFormatException e) {
            logger.debug("Unable to parse duration '{}' value as either a period or millisecond duration.", value);
            longValue = -1L;
          }
        }
        if (longValue > 0) {
          durationField.setValue(longValue.toString());
        }
        addField(durationField);
        break;
      case ITERABLE_TEXT:
        // Add an iterable text style field
        MetadataField<Iterable<String>> iterableTextField = MetadataField.createIterableStringMetadataField(
                metadataField.getInputID(), Opt.some(metadataField.getOutputID()), metadataField.getLabel(),
                metadataField.isReadOnly(), metadataField.isRequired(),
                getCollection(metadataField, listProvidersService), metadataField.getCollectionID(),
                metadataField.getOrder(), metadataField.getNamespace());
        if (StringUtils.isNotBlank(value)) {
          List<String> valueList = Arrays.asList(StringUtils.split(value, ","));
          iterableTextField.setValue(valueList);
        }
        addField(iterableTextField);
        break;
      case MIXED_TEXT:
        // Add an iterable text style field
        MetadataField<Iterable<String>> mixedIterableTextField = MetadataField.createMixedIterableStringMetadataField(
                metadataField.getInputID(), Opt.some(metadataField.getOutputID()), metadataField.getLabel(),
                metadataField.isReadOnly(), metadataField.isRequired(),
                getCollection(metadataField, listProvidersService), metadataField.getCollectionID(),
                metadataField.getOrder(), metadataField.getNamespace());
        if (StringUtils.isNotBlank(value)) {
          List<String> valueList = Arrays.asList(StringUtils.split(value, ","));
          mixedIterableTextField.setValue(valueList);
        }
        addField(mixedIterableTextField);
        break;
      case LONG:
        MetadataField<Long> longField = MetadataField.createLongMetadataField(metadataField.getInputID(),
                Opt.some(metadataField.getOutputID()), metadataField.getLabel(), metadataField.isReadOnly(),
                metadataField.isRequired(), getCollection(metadataField, listProvidersService),
                metadataField.getCollectionID(), metadataField.getOrder(), metadataField.getNamespace());
        if (StringUtils.isNotBlank(value)) {
          longField.setValue(Long.parseLong(value));
        }
        addField(longField);
        break;
      case TEXT:
        MetadataField<String> textField = MetadataField.createTextMetadataField(metadataField.getInputID(),
                Opt.some(metadataField.getOutputID()), metadataField.getLabel(), metadataField.isReadOnly(),
                metadataField.isRequired(), getCollection(metadataField, listProvidersService),
                metadataField.getCollectionID(), metadataField.getOrder(), metadataField.getNamespace());
        if (StringUtils.isNotBlank(value)) {
          textField.setValue(value);
        }
        addField(textField);
        break;
      case TEXT_LONG:
        MetadataField<String> textLongField = MetadataField.createTextLongMetadataField(metadataField.getInputID(),
                Opt.some(metadataField.getOutputID()), metadataField.getLabel(), metadataField.isReadOnly(),
                metadataField.isRequired(), getCollection(metadataField, listProvidersService),
                metadataField.getCollectionID(), metadataField.getOrder(), metadataField.getNamespace());
        if (StringUtils.isNotBlank(value)) {
          textLongField.setValue(value);
        }
        addField(textLongField);
        break;
      case START_DATE:
        if (metadataField.getPattern().isNone() || StringUtils.isBlank(metadataField.getPattern().get())) {
          throw new IllegalArgumentException("For temporal metadata field " + metadataField.getInputID() + " of type "
                  + metadataField.getType() + " there needs to be a pattern.");
        }
        MetadataField<String> startDate = MetadataField.createTemporalStartDateMetadata(metadataField.getInputID(),
                Opt.some(metadataField.getOutputID()), metadataField.getLabel(), metadataField.isReadOnly(),
                metadataField.isRequired(), metadataField.getPattern().get(), metadataField.getOrder(),
                metadataField.getNamespace());
        if (StringUtils.isNotBlank(value)) {
          startDate.setValue(value);
        }
        addField(startDate);
        break;
      case START_TIME:
        if (metadataField.getPattern().isNone() || StringUtils.isBlank(metadataField.getPattern().get())) {
          throw new IllegalArgumentException("For temporal metadata field " + metadataField.getInputID() + " of type "
                  + metadataField.getType() + " there needs to be a pattern.");
        }
        MetadataField<String> startTime = MetadataField.createTemporalStartTimeMetadata(metadataField.getInputID(),
                Opt.some(metadataField.getOutputID()), metadataField.getLabel(), metadataField.isReadOnly(),
                metadataField.isRequired(), metadataField.getPattern().get(), metadataField.getOrder(),
                metadataField.getNamespace());
        if (StringUtils.isNotBlank(value)) {
          startTime.setValue(value);
        }
        addField(startTime);
        break;
      default:
        throw new IllegalArgumentException("Unknown metadata type! " + metadataField.getType());
    }
  }
}
