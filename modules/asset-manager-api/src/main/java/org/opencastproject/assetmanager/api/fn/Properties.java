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
package org.opencastproject.assetmanager.api.fn;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Value;

import java.util.Date;

/**
 * Utility functions for dealing with single {@link Property properties} and property streams.
 */
public final class Properties {
  private Properties() {
  }

  /**
   * Set a string property on a media package.
   *
   */
  public static boolean setProperty(AssetManager am, String mpId, String namespace, String propertyName, String value) {
    return setProperty(am, mpId, namespace, propertyName, Value.mk(value));
  }

  /**
   * Set a date property on a media package.
   *
   */
  public static boolean setProperty(AssetManager am, String mpId, String namespace, String propertyName, Date value) {
    return setProperty(am, mpId, namespace, propertyName, Value.mk(value));
  }

  /**
   * Set a long property on a media package.
   *
   */
  public static boolean setProperty(AssetManager am, String mpId, String namespace, String propertyName, Long value) {
    return setProperty(am, mpId, namespace, propertyName, Value.mk(value));
  }

  /**
   * Set a boolean property on a media package.
   *
   */
  public static boolean setProperty(
      AssetManager am, String mpId, String namespace, String propertyName, boolean value) {
    return setProperty(am, mpId, namespace, propertyName, Value.mk(value));
  }

  /**
   * Set a property on a media package.
   *
   */
  public static boolean setProperty(AssetManager am, String mpId, String namespace, String propertyName, Value value) {
    return am.setProperty(Property.mk(PropertyId.mk(mpId, namespace, propertyName), value));
  }

  public static long removeProperties(AssetManager am, String mpId, String namespace) {
    return am.deletePropertiesWithCurrentUser(mpId, namespace);
  }

  /** Create a property. */
  public static Property mkProperty(String mpId, String namespace, String name, Value value) {
    return Property.mk(PropertyId.mk(mpId, namespace, name), value);
  }
}
