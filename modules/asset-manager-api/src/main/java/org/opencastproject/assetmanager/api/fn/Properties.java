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
package org.opencastproject.assetmanager.api.fn;

import static com.entwinemedia.fn.Equality.eq;
import static java.lang.String.format;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.PropertyName;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.Value.ValueType;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.PropertyField;
import org.opencastproject.mediapackage.MediaPackage;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Pred;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.StreamFold;
import com.entwinemedia.fn.StreamOp;
import com.entwinemedia.fn.data.Opt;

import java.util.Date;

/**
 * Utility functions for dealing with single {@link Property properties} and property streams.
 */
public final class Properties {
  private Properties() {
  }

  /**
   * Extract all properties contained in a result.
   * They'll appear in the order of the returned
   * {@linkplain org.opencastproject.assetmanager.api.query.ARecord records}.
   */
  public static Stream<Property> getProperties(AResult result) {
    return result.getRecords().bind(ARecords.getProperties);
  }

  /**
   * Create a predicate to query a property by its media package ID.
   *
   * @see PropertyId#getMediaPackageId()
   */
  public static Pred<Property> byMediaPackageId(final String id) {
    return new Pred<Property>() {
      @Override public Boolean apply(Property p) {
        return eq(p.getId().getMediaPackageId(), id);
      }
    };
  }

  /**
   * Create a predicate to query a property by its namespace.
   *
   * @see PropertyId#getNamespace()
   */
  public static Pred<Property> byNamespace(final String namespace) {
    return new Pred<Property>() {
      @Override public Boolean apply(Property p) {
        return eq(p.getId().getNamespace(), namespace);
      }
    };
  }

  /**
   * Create a predicate to query a property by its name.
   *
   * @see PropertyId#getName()
   */
  public static Pred<Property> byPropertyName(final String propertyName) {
    return new Pred<Property>() {
      @Override public Boolean apply(Property p) {
        return eq(p.getId().getName(), propertyName);
      }
    };
  }

  /**
   * Create a predicate to query a property by its full qualified name which is the tuple of namespace and name.
   *
   * @see PropertyId#getFqn()
   */
  public static Pred<Property> byFqnName(final PropertyName name) {
    return byNamespace(name.getNamespace()).and(byPropertyName(name.getName()));
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

  public static Opt<Property> getProperty(AssetManager am, String mpId, String namespace, String propertyName) {
    final AQueryBuilder q = am.createQuery();
    return q.select(q.properties(PropertyName.mk(namespace, propertyName)))
            .where(q.mediaPackageId(mpId).and(q.property(Value.UNTYPED, namespace, propertyName).exists()))
            .run()
            .getRecords().bind(ARecords.getProperties).head();
  }

  /**
   * {@link Property#getValue()} as a function.
   */
  public static final Fn<Property, Value> getValue = new Fn<Property, Value>() {
    @Override public Value apply(Property p) {
      return p.getValue();
    }
  };

  /**
   * Create a function to get a value from a property.
   *
   * @param ev the expected value type
   */
  public static <A> Fn<Property, A> getValue(final ValueType<A> ev) {
    return new Fn<Property, A>() {
      @Override public A apply(Property p) {
        return p.getValue().get(ev);
      }
    };
  }

  /**
   * Create a stream fold to find the first property whose {@linkplain PropertyId#getName() name} matches the given one
   * and extract its value.
   *
   * @param ev the expected value type
   * @param propertyName the name of the property
   * @throws RuntimeException if the property cannot be found or its type does not match
   */
  public static <A> StreamFold<Property, A> getValue(final ValueType<A> ev, final String propertyName) {
    return StreamFold.find(byPropertyName(propertyName)).fmap(get(propertyName)).fmap(getValue(ev));
  }

  /**
   * Create a stream fold to find the first property whose
   * {@linkplain PropertyId#getFqn() full qualified name} matches the given one
   * and extract its value.
   *
   * @param ev the expected value type
   * @param name the full qualified name of the property
   * @throws RuntimeException if the property cannot be found or its type does not match
   */
  public static <A> StreamFold<Property, A> getValue(final ValueType<A> ev, final PropertyName name) {
    return StreamFold.find(byFqnName(name)).fmap(get(name)).fmap(getValue(ev));
  }

  /**
   * Create a stream fold to find the first property whose {@linkplain PropertyId#getName() name} matches the given one
   * and extract its value, wrapped in an {@link Opt}.
   *
   * @param ev the expected value type
   * @param propertyName the name of the property
   */
  public static <A> StreamFold<Property, Opt<A>> getValueOpt(final ValueType<A> ev, final String propertyName) {
    return StreamFold.find(byPropertyName(propertyName)).fmap(lift(getValue(ev)));
  }

  /**
   * Create a stream fold to find the first property whose
   * {@linkplain PropertyId#getFqn() full qualified name} matches the given one
   * and extract their values, wrapped in an {@link Opt}.
   *
   * @param ev the expected value type
   * @param name the full qualified name of the property
   */
  public static <A> StreamFold<Property, Opt<A>> getValueOpt(final ValueType<A> ev, final PropertyName name) {
    return StreamFold.find(byFqnName(name)).fmap(lift(getValue(ev)));
  }

  /**
   * Apply #get() to the given Opt or throw a RuntimeException if none.
   * Use <code>propertyName</code> for the exception message.
   */
  public static Fn<Opt<Property>, Property> get(final String propertyName) {
    return new Fn<Opt<Property>, Property>() {
      @Override public Property apply(Opt<Property> p) {
        for (Property pp : p) {
          return pp;
        }
        throw new RuntimeException(format("Property [%s] does not exist", propertyName));
      }
    };
  }

  /**
   * Apply #get() to the given Opt or throw a RuntimeException if none.
   * Use <code>name</code> for the exception message.
   */
  public static Fn<Opt<Property>, Property> get(final PropertyName name) {
    return get(name.toString());
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

  /* -------------------------------------------------------------------------------------------------------------- */

  /**
   * Get a boolean value. Uses the first property with the given name.
   *
   * @throws java.lang.RuntimeException
   *         if the property does not exist or if the property is not a boolean
   */
  public static StreamFold<Property, Boolean> getBoolean(final String propertyName) {
    return getValue(Value.BOOLEAN, propertyName);
  }

  /**
   * Get a boolean value. Uses the first property with the given name.
   *
   * @throws java.lang.RuntimeException
   *         if the property does not exist or if the property is not a boolean
   */
  public static StreamFold<Property, Boolean> getBoolean(final PropertyName name) {
    return getValue(Value.BOOLEAN, name);
  }

  /* -------------------------------------------------------------------------------------------------------------- */

  /**
   * Get a string value. Uses the first property with the given name.
   *
   * @throws java.lang.RuntimeException
   *         if the property does not exist or if the property is not a string
   */
  public static StreamFold<Property, String> getString(final String propertyName) {
    return getValue(Value.STRING, propertyName);
  }

  /**
   * Get a string value. Uses the first property with the given name.
   *
   * @throws java.lang.RuntimeException
   *         if the property does not exist or if the property is not a string
   */
  public static StreamFold<Property, String> getString(final PropertyName name) {
    return getValue(Value.STRING, name);
  }

  /**
   * Get string values from all properties.
   *
   * @throws java.lang.RuntimeException
   *         if at least one property is not a string
   */
  public static StreamOp<Property, String> getStrings(final String propertyName) {
    return new StreamOp<Property, String>() {
      @Override public Stream<String> apply(Stream<? extends Property> s) {
        return s.filter(byPropertyName(propertyName)).map(getValue(Value.STRING));
      }
    };
  }

  /**
   * Get string values from all properties.
   *
   * @throws java.lang.RuntimeException
   *         if at least one property is not a string
   */
  public static StreamOp<Property, String> getStrings(final PropertyName name) {
    return new StreamOp<Property, String>() {
      @Override public Stream<String> apply(Stream<? extends Property> s) {
        return s.filter(byFqnName(name)).map(getValue(Value.STRING));
      }
    };
  }

  /* -------------------------------------------------------------------------------------------------------------- */

  /**
   * Get a date value. Uses the first property with the given name.
   *
   * @throws java.lang.RuntimeException
   *         if the property does not exist or if the property is not a date
   */
  public static StreamFold<Property, Date> getDate(final String propertyName) {
    return getValue(Value.DATE, propertyName);
  }

  /**
   * Get a date value. Uses the first property with the given name.
   *
   * @throws java.lang.RuntimeException
   *         if the property does not exist or if the property is not a date
   */
  public static StreamFold<Property, Date> getDate(final PropertyName name) {
    return getValue(Value.DATE, name);
  }

  /* -------------------------------------------------------------------------------------------------------------- */

  /**
   * Get a long value. Uses the first property with the given name.
   *
   * @throws java.lang.RuntimeException
   *         if the property does not exist or if the property is not a long
   */
  public static StreamFold<Property, Long> getLong(final String propertyName) {
    return getValue(Value.LONG, propertyName);
  }

  /**
   * Get a long value. Uses the first property with the given name.
   *
   * @throws java.lang.RuntimeException
   *         if the property does not exist or if the property is not a long
   */
  public static StreamFold<Property, Long> getLong(final PropertyName name) {
    return getValue(Value.LONG, name);
  }

  /* -------------------------------------------------------------------------------------------------------------- */

  /**
   * Get a string value. Uses the first property with the given name.
   *
   * @throws java.lang.RuntimeException
   *         if the property is not a string
   */
  public static StreamFold<Property, Opt<String>> getStringOpt(final String propertyName) {
    return getValueOpt(Value.STRING, propertyName);
  }

  /**
   * Get a string value. Uses the first property with the given name.
   *
   * @throws java.lang.RuntimeException
   *         if the property is not a string
   */
  public static StreamFold<Property, Opt<String>> getStringOpt(final PropertyName name) {
    return getValueOpt(Value.STRING, name);
  }

  /* -------------------------------------------------------------------------------------------------------------- */

  /**
   * Get a date value. Uses the first property with the given name.
   *
   * @throws java.lang.RuntimeException
   *         if the property is not a date
   */
  public static StreamFold<Property, Opt<Date>> getDateOpt(final String propertyName) {
    return getValueOpt(Value.DATE, propertyName);
  }

  /**
   * Get a date value. Uses the first property with the given name.
   *
   * @throws java.lang.RuntimeException
   *         if the property is not a date
   */
  public static StreamFold<Property, Opt<Date>> getDateOpt(final PropertyName name) {
    return getValueOpt(Value.DATE, name);
  }

  /* -------------------------------------------------------------------------------------------------------------- */

  /**
   * Get a long value. Uses the first property with the given name.
   *
   * @throws java.lang.RuntimeException
   *         if the property is not a long
   */
  public static StreamFold<Property, Opt<Long>> getLongOpt(final String propertyName) {
    return getValueOpt(Value.LONG, propertyName);
  }

  /**
   * Get a long value. Uses the first property with the given name.
   *
   * @throws java.lang.RuntimeException
   *         if the property is not a long
   */
  public static StreamFold<Property, Opt<Long>> getLongOpt(final PropertyName name) {
    return getValueOpt(Value.LONG, name);
  }

  /* -------------------------------------------------------------------------------------------------------------- */

  private static <A, B> Fn<Opt<A>, Opt<B>> lift(final Fn<? super A, ? extends B> f) {
    return new Fn<Opt<A>, Opt<B>>() {
      @Override public Opt<B> apply(Opt<A> as) {
        return as.map(f);
      }
    };
  }
}
