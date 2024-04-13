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
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.PropertyField;
import org.opencastproject.mediapackage.MediaPackage;

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
   * @deprecated make use of a {@link org.opencastproject.assetmanager.api.query.PropertySchema}
   * instead of creating property IDs manually
   */
  public static boolean setProperty(AssetManager am, String mpId, String namespace, String propertyName, String value) {
    return setProperty(am, mpId, namespace, propertyName, Value.mk(value));
  }

  /**
   * Set a date property on a media package.
   *
   * @deprecated make use of a {@link org.opencastproject.assetmanager.api.query.PropertySchema}
   * instead of creating property IDs manually
   */
  public static boolean setProperty(AssetManager am, String mpId, String namespace, String propertyName, Date value) {
    return setProperty(am, mpId, namespace, propertyName, Value.mk(value));
  }

  /**
   * Set a long property on a media package.
   *
   * @deprecated make use of a {@link org.opencastproject.assetmanager.api.query.PropertySchema}
   * instead of creating property IDs manually
   */
  public static boolean setProperty(AssetManager am, String mpId, String namespace, String propertyName, Long value) {
    return setProperty(am, mpId, namespace, propertyName, Value.mk(value));
  }

  /**
   * Set a boolean property on a media package.
   *
   * @deprecated make use of a {@link org.opencastproject.assetmanager.api.query.PropertySchema}
   * instead of creating property IDs manually
   */
  public static boolean setProperty(
      AssetManager am, String mpId, String namespace, String propertyName, boolean value) {
    return setProperty(am, mpId, namespace, propertyName, Value.mk(value));
  }

  /**
   * Set a property on a media package.
   *
   * @deprecated make use of a {@link org.opencastproject.assetmanager.api.query.PropertySchema}
   * instead of creating property IDs manually
   */
  public static boolean setProperty(AssetManager am, String mpId, String namespace, String propertyName, Value value) {
    return am.setProperty(Property.mk(PropertyId.mk(mpId, namespace, propertyName), value));
  }

  public static long removeProperties(AssetManager am, String owner, String orgId, String mpId, String namespace) {
    final AQueryBuilder q = am.createQuery();
    return q.delete(owner, q.propertiesOf(namespace)).where(q.organizationId(orgId).and(q.mediaPackageId(mpId))).run();
  }

  /** Create a property. */
  public static <A> Property mkProperty(PropertyField<A> f, MediaPackage mp, A value)  {
    return f.mk(mp.getIdentifier().toString(), value);
  }

  /** Create a property. */
  public static <A> Property mkProperty(PropertyField<A> f, Snapshot e, A value)  {
    return f.mk(e.getMediaPackage().getIdentifier().toString(), value);
  }

  /** Create a property. */
  public static Property mkProperty(String mpId, String namespace, String name, Value value) {
    return Property.mk(PropertyId.mk(mpId, namespace, name), value);
  }
}
