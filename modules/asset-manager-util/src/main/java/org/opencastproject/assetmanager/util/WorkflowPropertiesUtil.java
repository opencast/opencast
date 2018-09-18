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
package org.opencastproject.assetmanager.util;

import static org.opencastproject.assetmanager.api.AssetManager.DEFAULT_OWNER;
import static org.opencastproject.systems.OpencastConstants.WORKFLOW_PROPERTIES_NAMESPACE;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.mediapackage.MediaPackage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to store and retrieve Workflow Properties (which are stored in specially prefixed Asset Manager
 * properties)
 */
public final class WorkflowPropertiesUtil {
  private WorkflowPropertiesUtil() {
  }

  /**
   * Retrieve latest properties for a set of event ids
   * @param assetManager The Asset Manager to use
   * @param eventIds Collection of event IDs (can be a set, but doesn't have to be)
   * @return A map mapping event IDs to key value pairs (which are themselves maps) representing the properties
   */
  public static Map<String, Map<String, String>> getLatestWorkflowPropertiesForEvents(final AssetManager assetManager,
          final Collection<String> eventIds) {
    final AQueryBuilder query = assetManager.createQuery();
    final AResult result = query.select(query.snapshot(), query.propertiesOf(WORKFLOW_PROPERTIES_NAMESPACE))
            .where(query.mediaPackageIds(eventIds.toArray(new String[0])).and(query.version().isLatest())).run();
    final Map<String, Map<String, String>> workflowProperties = new HashMap<>(eventIds.size());
    for (final ARecord record : result.getRecords().toList()) {
      final List<Property> recordProps = record.getProperties().toList();
      final Map<String, String> eventMap = new HashMap<>(recordProps.size());
      for (final Property property : recordProps) {
        eventMap.put(property.getId().getName(), property.getValue().get(Value.STRING));
      }
      final String eventId = record.getMediaPackageId();
      workflowProperties.put(eventId, eventMap);
    }
    return workflowProperties;
  }

  /**
   * Retrieve the latest properties for a single media package
   * @param assetManager The Asset Manager to use
   * @param mediaPackageId The media package to query
   * @return A list of properties represented by a Map
   */
  public static Map<String, String> getLatestWorkflowProperties(final AssetManager assetManager,
          final String mediaPackageId) {
    final AQueryBuilder query = assetManager.createQuery();
    final List<ARecord> queryResults = query.select(query.snapshot(), query.propertiesOf(WORKFLOW_PROPERTIES_NAMESPACE))
            .where(query.mediaPackageId(mediaPackageId).and(query.version().isLatest())).run()
            .getRecords().toList();
    final Map<String, String> workflowParameters = new HashMap<>(0);
    if (!queryResults.isEmpty()) {
      for (final Property property : queryResults.get(0).getProperties()) {
        workflowParameters.put(property.getId().getName(), property.getValue().get(Value.STRING));
      }
    }
    return workflowParameters;
  }

  /**
   * Store selected properties for a media package
   * @param assetManager The Asset Manager to use
   * @param mediaPackage The media package to store properties relative to
   * @param properties A list of properties represented by a Map
   */
  public static void storeProperties(final AssetManager assetManager, final MediaPackage mediaPackage,
          final Map<String, String> properties) {
    assetManager.takeSnapshot(DEFAULT_OWNER,mediaPackage);
    for (final Map.Entry<String, String> entry : properties.entrySet()) {
      final PropertyId propertyId = PropertyId
              .mk(mediaPackage.getIdentifier().compact(), WORKFLOW_PROPERTIES_NAMESPACE, entry.getKey());
      final Property property = Property.mk(propertyId, Value.mk(entry.getValue()));
      assetManager.setProperty(property);
    }
  }

  public static void storeProperty(final AssetManager assetManager, final MediaPackage mediaPackage,
          final String name, final String value) {
    storeProperties(assetManager, mediaPackage, Collections.singletonMap(name, value));
  }
}
