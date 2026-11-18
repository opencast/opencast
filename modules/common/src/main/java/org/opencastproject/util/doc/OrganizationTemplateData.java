/*
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

package org.opencastproject.util.doc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds organization properties to a FreeMarker template data model under the {@code org_} prefix.
 *
 * <p>
 * This mirrors the organization property substitution the workflow service applies to operation configuration
 * values: a property {@code my.var} is available as {@code ${org_my.var}}, the form documented in the workflow
 * configuration guide. Since FreeMarker can only address unbroken names, the property is additionally available in
 * a flattened form, {@code ${org_my_var}}.
 */
public final class OrganizationTemplateData {

  /** The template key prefix for organization properties, as resolved by the workflow service */
  public static final String ORG_TEMPLATE_KEY_PREFIX = "org_";

  private OrganizationTemplateData() {
    // Utility class
  }

  /**
   * Adds the given organization properties to the template data model under the {@code org_} prefix.
   *
   * @param dataModel
   *          the template data model to add the properties to
   * @param organizationProperties
   *          the organization properties, keyed by their property name (e.g. {@code my.var}), or {@code null}
   */
  public static void addOrganizationProperties(Map<String, Object> dataModel,
          Map<String, String> organizationProperties) {
    if (organizationProperties == null) {
      return;
    }
    for (Map.Entry<String, String> property : organizationProperties.entrySet()) {
      dataModel.put(ORG_TEMPLATE_KEY_PREFIX + property.getKey().replace('.', '_').toLowerCase(),
          property.getValue());
    }
    // The dotted form is addressed through a nested hash. If one property name is a dot prefix of another, only one
    // of the two can have a dotted form: the longer keys are processed first, so a dotted shorter key (a.b next to
    // a.b.c) keeps only its flattened form, while an undotted shorter key (player next to player.mobile) already
    // occupies the root with its flattened form and it is the longer key that keeps only its flattened form
    List<Map.Entry<String, String>> properties = new ArrayList<>(organizationProperties.entrySet());
    properties.sort((a, b) -> b.getKey().length() - a.getKey().length());
    for (Map.Entry<String, String> property : properties) {
      String[] segments = (ORG_TEMPLATE_KEY_PREFIX + property.getKey()).split("\\.");
      if (segments.length < 2) {
        continue;
      }
      Map<String, Object> node = dataModel;
      boolean blocked = false;
      for (int i = 0; i < segments.length - 1; i++) {
        Object next = node.get(segments[i]);
        if (next instanceof Map) {
          node = (Map<String, Object>) next;
        } else if (next == null) {
          Map<String, Object> child = new HashMap<>();
          node.put(segments[i], child);
          node = child;
        } else {
          // A value, e.g. the flattened form of another property, occupies this position
          blocked = true;
          break;
        }
      }
      if (!blocked) {
        Object leaf = node.get(segments[segments.length - 1]);
        if (!(leaf instanceof Map)) {
          node.put(segments[segments.length - 1], property.getValue());
        }
      }
    }
  }
}
