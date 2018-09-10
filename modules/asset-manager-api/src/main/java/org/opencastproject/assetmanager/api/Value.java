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

import static com.entwinemedia.fn.Equality.eq;
import static com.entwinemedia.fn.Equality.hash;
import static java.lang.String.format;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.P1;
import com.entwinemedia.fn.Prelude;

import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * A property value.
 * <p>
 * The wrapped type is not exposed as a generic type parameter since {@link Value}s appear in contexts like lists where this type information cannot be preserved.
 * To access the wrapped type one can choose between two options.
 * If the type is known, use {@link #get(ValueType)}.
 * If the type is not known, safely decompose the value with {@link #decompose(Fn, Fn, Fn, Fn, Fn)}.
 * </p>
 * The value type is a sum type made up from
 * <ul>
 * <li>{@link StringValue}
 * <li>{@link DateValue}
 * <li>{@link LongValue}
 * <li>{@link BooleanValue}
 * </ul>
 * <p>
 * Use one of the various <code>mk(..)</code> constructors to create a new {@link Value}.
 *
 * @see Property
 */
@ParametersAreNonnullByDefault
public abstract class Value {
  public static final StringType STRING = new StringType();
  public static final DateType DATE = new DateType();
  public static final LongType LONG = new LongType();
  public static final BooleanType BOOLEAN = new BooleanType();
  public static final VersionType VERSION = new VersionType();
  // TODO: rename to UNKNOWN
  public static final UntypedType UNTYPED = new UntypedType();
//  public static final Class<UntypedValue> UNTYPED = UntypedValue.class;

  private Value() {
  }

  /** Get the wrapped value. */
  public abstract Object get();

  /**
   * Get the wrapped value in a type safe way. Use this method if you are
   * sure about the contained value type. Otherwise consider the use
   * of {@link #decompose(Fn, Fn, Fn, Fn, Fn)}.
   *
   * @param ev
   *         Evidence type. The type parameter <code>A</code> of the evidence type
   *         must match the type of the wrapped value. Any other value will result
   *         in an exception being thrown.
   * @throws java.lang.RuntimeException
   *         if the passed evidence <code>ev</code> does not match the type of the wrapped value
   */
  public final <A> A get(ValueType<A> ev) {
    if (getType().getClass().equals(ev.getClass())) {
      return (A) get();
    } else {
      throw new RuntimeException(this + " is not a " + ev.getClass().getSimpleName());
    }
  }

  public final ValueType<?> getType() {
    return decompose(new Fn<String, ValueType<?>>() {
      @Override public ValueType<?> apply(String a) {
        return STRING;
      }
    }, new Fn<Date, ValueType<?>>() {
      @Override public ValueType<?> apply(Date a) {
        return DATE;
      }
    }, new Fn<Long, ValueType<?>>() {
      @Override public ValueType<?> apply(Long a) {
        return LONG;
      }
    }, new Fn<Boolean, ValueType<?>>() {
      @Override public ValueType<?> apply(Boolean a) {
        return BOOLEAN;
      }
    }, new Fn<Version, ValueType<?>>() {
      @Override public ValueType<?> apply(Version a) {
        return VERSION;
      }
    });
  }

  /**
   * Decompose (or pattern match) the value instance. Provide a function to handle each possible type.
   * Use {@link #doNotMatch()} as a placeholder that yields an {@link Prelude#unexhaustiveMatch()} error.
   */
  public final <A> A decompose(
          Fn<? super String, ? extends A> stringValue,
          Fn<? super Date, ? extends A> dateValue,
          Fn<? super Long, ? extends A> longValue,
          Fn<? super Boolean, ? extends A> booleanValue,
          Fn<? super Version, ? extends A> versionValue) {
    if (this instanceof StringValue) {
      return stringValue.apply(((StringValue) this).get());
    } else if (this instanceof DateValue) {
      return dateValue.apply(((DateValue) this).get());
    } else if (this instanceof LongValue) {
      return longValue.apply(((LongValue) this).get());
    } else if (this instanceof BooleanValue) {
      return booleanValue.apply(((BooleanValue) this).get());
    } else if (this instanceof VersionValue) {
      return versionValue.apply(((VersionValue) this).get());
    } else {
      // catch bug
      return Prelude.unexhaustiveMatch(this);
    }
  }

  //

  /**
   * Use as a placeholder that yields an {@link Prelude#unexhaustiveMatch()} error in
   * value decomposition.
   *
   * @see #decompose(Fn, Fn, Fn, Fn, Fn)
   */
  public static <B> Fn<Object, B> doNotMatch() {
    return new Fn<Object, B>() {
      @Override public B apply(Object a) {
        return Prelude.unexhaustiveMatch(a);
      }
    };
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  //
  // Type evidence and factory classes
  //

  /**
   * ValueType gives evidence that type <code>A</code> is suitable for the creation
   * of a {@link Value}.
   * <p>
   * This is a more advanced version of the usual <code>Class&lt;A&gt;</code> idiom.
   * A <code>ValueType</code> is also a constructor for {@link TypedValue}s of
   * the same type <code>A</code>.
   *
   * @param <A> the type to give evidence of
   */
  public abstract static class ValueType<A> {
    /** It should not be possible to inherit from outside class {@link Value}. */
    private ValueType() {
    }

    public abstract TypedValue<A> mk(A a);

    public abstract <B> B match(
            P1<? extends B> stringType,
            P1<? extends B> dateType,
            P1<? extends B> longType,
            P1<? extends B> booleanType,
            P1<? extends B> versionType);
  }

  public static final class StringType extends ValueType<String> {
    @Override public TypedValue<String> mk(String a) {
      return Value.mk(a);
    }

    @Override public <B> B match(
            P1<? extends B> stringType,
            P1<? extends B> dateType,
            P1<? extends B> longType,
            P1<? extends B> booleanType,
            P1<? extends B> versionType) {
      return stringType.get1();
    }
  }

  public static final class DateType extends ValueType<Date> {
    @Override public TypedValue<Date> mk(Date a) {
      return Value.mk(a);
    }

    @Override public <B> B match(
            P1<? extends B> stringType,
            P1<? extends B> dateType,
            P1<? extends B> longType,
            P1<? extends B> booleanType,
            P1<? extends B> versionType) {
      return dateType.get1();
    }
  }

  public static final class LongType extends ValueType<Long> {
    @Override public TypedValue<Long> mk(Long a) {
      return Value.mk(a);
    }

    @Override public <B> B match(
            P1<? extends B> stringType,
            P1<? extends B> dateType,
            P1<? extends B> longType,
            P1<? extends B> booleanType,
            P1<? extends B> versionType) {
      return longType.get1();
    }
  }

  public static final class BooleanType extends ValueType<Boolean> {
    @Override public TypedValue<Boolean> mk(Boolean a) {
      return Value.mk(a);
    }

    @Override public <B> B match(
            P1<? extends B> stringType,
            P1<? extends B> dateType,
            P1<? extends B> longType,
            P1<? extends B> booleanType,
            P1<? extends B> versionType) {
      return booleanType.get1();
    }
  }

  public static final class VersionType extends ValueType<Version> {
    @Override public TypedValue<Version> mk(Version a) {
      return Value.mk(a);
    }

    @Override public <B> B match(
            P1<? extends B> stringType,
            P1<? extends B> dateType,
            P1<? extends B> longType,
            P1<? extends B> booleanType,
            P1<? extends B> versionType) {
      return versionType.get1();
    }
  }

  public static final class UntypedType extends ValueType<Object> {
    @Override public TypedValue<Object> mk(Object a) {
      throw new RuntimeException("Cannot create an untyped value");
    }

    @Override public <B> B match(
            P1<? extends B> stringType,
            P1<? extends B> dateType,
            P1<? extends B> longType,
            P1<? extends B> booleanType,
            P1<? extends B> versionType) {
      throw new RuntimeException("Cannot match an untyped value type");
    }
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  //
  // Value classes
  //

  /** Helper type to reduce boilerplate code. */
  // CHECKSTYLE:OFF -> class shall be public but not the constructor
  public static class TypedValue<A> extends Value {
    private final A value;

    /** It should not be possible to inherit from outside class {@link Value}. */
    private TypedValue(@Nonnull A value) {
      this.value = value;
    }

    @Override public A get() {
      return value;
    }

    @Override public int hashCode() {
      return hash(value);
    }

    // generic implementation of equals
    // since all wrapped types cannot equal each other this is safe
    @Override public boolean equals(Object that) {
      return (this == that) || (that instanceof TypedValue && eqFields((TypedValue) that));
    }

    private boolean eqFields(TypedValue that) {
      return eq(value, that.value);
    }

    @Override public String toString() {
      return format("%s(%s)", getClass().getSimpleName(), value);
    }
  }
  // CHECKSTYLE:ON

  /**
   * A value of type {@link String}.
   */
  @Immutable
  public static final class StringValue extends TypedValue<String> {
    public StringValue(@Nonnull String value) {
      super(value);
    }
  }

  /**
   * A value of type {@link java.util.Date}.
   */
  public static final class DateValue extends TypedValue<Date> {
    public DateValue(@Nonnull Date value) {
      super(value);
    }
  }

  /**
   * A value of type {@link java.lang.Long}.
   */
  @Immutable
  public static final class LongValue extends TypedValue<Long> {
    public LongValue(@Nonnull Long value) {
      super(value);
    }
  }

  /**
   * A value of type {@link java.lang.Boolean}.
   */
  @Immutable
  public static final class BooleanValue extends TypedValue<Boolean> {
    public BooleanValue(@Nonnull Boolean value) {
      super(value);
    }
  }

  /**
   * A value of type {@link Version}.
   */
  @Immutable
  public static final class VersionValue extends TypedValue<Version> {
    public VersionValue(@Nonnull Version value) {
      super(value);
    }
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  //
  // constructor methods
  //

  /** Create a new value of type {@link String}. */
  public static StringValue mk(String value) {
    return new StringValue(value);
  }

  /** Create a new value of type {@link java.util.Date}. */
  public static DateValue mk(Date value) {
    return new DateValue(value);
  }

  /** Create a new value of type {@link java.lang.Long}. */
  public static LongValue mk(Long value) {
    return new LongValue(value);
  }

  /** Create a new value of type {@link java.lang.Boolean}. */
  public static BooleanValue mk(Boolean value) {
    return new BooleanValue(value);
  }

  /** Create a new value of type {@link Version}. */
  public static VersionValue mk(Version value) {
    return new VersionValue(value);
  }

  /** Generic constructor. Creates a value for any existing ValueType. */
  public static <A> TypedValue<A> mk(ValueType<A> mk, A a) {
    return mk.mk(a);
  }
}
