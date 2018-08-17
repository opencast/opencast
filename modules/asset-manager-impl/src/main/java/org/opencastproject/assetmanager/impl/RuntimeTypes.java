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
package org.opencastproject.assetmanager.impl;

import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.ADeleteQuery;
import org.opencastproject.assetmanager.api.query.Order;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.api.query.PropertyField;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.impl.query.AbstractADeleteQuery;
import org.opencastproject.assetmanager.impl.query.AbstractOrder;
import org.opencastproject.assetmanager.impl.query.AbstractPredicate;
import org.opencastproject.assetmanager.impl.query.AbstractTarget;
import org.opencastproject.assetmanager.impl.query.PropertyFieldImpl;

import com.entwinemedia.fn.Fn;

/**
 * This implementation of the AssetManager only takes its own implementation of query, result, record etc.
 * This could by modeled in a static, type safe manner with path dependent types but
 * this concept is not known to Java so these checks have to be performed at runtime.
 * <p>
 * All those checks are grouped in this class.
 */
public final class RuntimeTypes {
  private RuntimeTypes() {
  }

  /**
   * {@link #convert(Version)} as a function.
   */
  public static final Fn<Version, VersionImpl> toVersionImpl = new Fn<Version, VersionImpl>() {
    @Override public VersionImpl apply(Version version) {
      return convert(version);
    }
  };

  /**
   * Try to cast a Version into a VersionImpl. Throw a {@link AssetManagerException} in case of failure.
   */
  public static VersionImpl convert(Version a) {
    return cast(VersionImpl.class, a);
  }

  public static AbstractPredicate convert(Predicate a) {
    return cast(AbstractPredicate.class, a);
  }

  public static AbstractTarget convert(Target a) {
    return cast(AbstractTarget.class, a);
  }

  public static AbstractOrder convert(Order a) {
    return cast(AbstractOrder.class, a);
  }

  public static AbstractADeleteQuery convert(ADeleteQuery a) {
    return cast(AbstractADeleteQuery.class, a);
  }

  public static <A> PropertyFieldImpl<A> convert(PropertyField<A> a) {
    return cast(PropertyFieldImpl.class, a);
  }

  private static <B, A extends B> A cast(Class<A> ev, B b) {
    try {
      return (A) b;
    } catch (ClassCastException e) {
      throw new AssetManagerException("This AssetManager implementation only handles type " + ev);
    }
  }
}
