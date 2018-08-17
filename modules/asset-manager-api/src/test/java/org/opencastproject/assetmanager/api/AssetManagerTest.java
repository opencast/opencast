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

package org.opencastproject.assetmanager.api;

import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.PropertyField;
import org.opencastproject.assetmanager.api.query.PropertySchema;

import org.junit.Test;

import java.util.Date;

public class AssetManagerTest {
  /**
   * Does not run. Only to test compilation.
   */
  @Test(expected = java.lang.NullPointerException.class)
  public void testAssetManagerApiCompilation() {
    AssetManager am = null;
    AQueryBuilder q = am.createQuery();

    //
    // Selection
    //

    // Select all versions of media package "mp-id".
    // Fetch media packages and associated metadata like version, creation date etc.
    // Do not fetch any properties since the query does not contain a property target.
    q.select(q.snapshot()).where(q.mediaPackageId("mp-id")).run();

    // Select the latest version of "mp-id".
    // Fetch the media package and associated metadata like version, creation date etc.
    // Do not fetch any properties since the query does not contain a property target.
    q.select(q.snapshot()).where(q.mediaPackageId("mp-id").and(q.version().isLatest())).run();

    // Select the latest version of "mp-id".
    // Fetch the media package and associated metadata like version, creation date etc.
    // Also fetch _all_ properties of the media package.
    q.select(q.snapshot(), q.properties()).where(q.mediaPackageId("some-mp-id").and(q.version().isLatest())).run();

    // Select the latest version of "mp-id" only if it has properties of namespace "org.opencastproject.service" attached.
    // Fetch only the media package and associated metadata like version, creation date etc.
    q.select(q.snapshot()).where(q.mediaPackageId("mp-id").and(q.version().isLatest()).and(q.hasPropertiesOf("org.opencastproject.service"))).run();

    // Select all versions of media package "mp-id" that are archived before now. Do not fetch associated properties.
    final Date date = null; // <- assume some real date here
    q.select(q.snapshot()).where(q.mediaPackageId("mp-id").and(q.archived().lt(date))).run();

    // does not compile because of a type error
    //AssetResult r = q.select(q.mediaPackage("some-mp-id").and(q.archived().lt(q.val(Value.mk("bla"))))).fetchProperties(false).run();

    // Select all versions of all media packages of series "series".
    // Also fetch all of their properties.
    q.select(q.snapshot()).where(q.seriesId().eq("series")).run();

    // Select the latest version of each media package of series "series".
    // Also fetch all of their properties.
    q.select(q.snapshot()).where(q.seriesId().eq("series").and(q.version().isLatest())).run();

    // Select all media packages and all of their versions that have boolean property "org.opencastproject.myservice:approved" set to true.
    // Fetch media packages and associated metadata like version, creation date etc.
    // Fetch all properties of namespace "org.opencastproject.myservice"
    q.select(q.snapshot(), q.propertiesOf("org.opencastproject.myservice"))
            .where(q.property(Value.BOOLEAN, "org.opencastproject.myservice", "approved").eq(true)).run();

    // The above works but it's better to define and use a property schema!
    final Props p = new Props(q);

    // Select all versions of media packages that have boolean property "org.opencastproject.myservice:approved" set to true.
    q.select(q.snapshot()).where(p.approved().eq(true)).run();

    // Select all versions of media packages and properties of namespace "org.opencastproject.myservice" having a "count" property of less than 10.
    q.select(q.snapshot(), q.propertiesOf(p.namespace())).where(p.count().lt(10L)).run();

    final Version v = null; // <- assume some version here
    // Select all versions of all media packages having a younger version than "v".
    // Order the result set after their archival date.
    q.select(q.snapshot()).where(q.version().lt(v)).orderBy(q.archived().asc()).run();

    // Select properties of namespace "org.opencastproject.myservice" of media package "mp-id".
    q.select(p.allProperties()).where(q.mediaPackageId("mp-id")).run();

    // Select property "org.opencastproject.myservice:count" of media package "mp-id".
    q.select(p.count().target()).where(q.mediaPackageId("mp-id")).run();

    // Select all media packages and their start property which start in the future.
    q.select(q.snapshot(), p.start().target()).where(p.start().gt(new Date())).run();

    //
    // Deletion
    //

    // Delete all properties of all media packages.
    q.delete("owner", q.properties()).run();

    // Delete all properties of namespace "org.opencastproject.myservice" of all media packages.
    q.delete("owner", q.propertiesOf("org.opencastproject.myservice")).run();

    // Delete all properties of namespace "org.opencastproject.myservice" from media package "mp-id".
    q.delete("owner", q.propertiesOf("org.opencastproject.myservice")).where(q.mediaPackageId("mp-id")).run();

    // Delete property "org.opencastproject.myservice:prop" from media package "mp-id".
    q.delete("owner", q.properties(PropertyName.mk("org.opencastproject.myservice", "prop"))).where(q.mediaPackageId("mp-id")).run();

    // Delete properties "org.opencastproject.myservice:prop" and "org.opencastproject.myservice:prop2" from media package "mp-id".
    q.delete("owner", q.properties(PropertyName.mk("org.opencastproject.myservice", "prop"), PropertyName.mk("org.opencastproject.myservice", "prop2"))).where(q.mediaPackageId("mp-id")).run();

    // Delete all "org.opencastproject.myservice:approved" properties from all media package having a "org.opencastproject.myservice:count" greater 10.
    q.delete("owner", p.approved().target()).where(p.count().gt(10L));

    //
    // Properties
    //

    // Set property "org.opencastproject.myservice:count" of media package "mp-id" to 10.
    am.setProperty(Property.mk(PropertyId.mk("mp-id", "org.opencastproject.myservice", "count"), Value.mk(10L)));

    // Set property "org.opencastproject.myservice:count" of media package "mp-id" to 10 using the schema definition.
    am.setProperty(p.count().mk("mp-id", 10L));
  }

  /** A service's sample property schema. */
  static class Props extends PropertySchema {
    Props(AQueryBuilder q) {
      super(q, "org.opencastproject.myservice");
    }

    // define your properties here below

    public PropertyField<Long> count() {
      return longProp("count");
    }

    public PropertyField<Boolean> approved() {
      return booleanProp("approved");
    }

    public PropertyField<Date> start() {
      return dateProp("start");
    }
  }
}

