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


package org.opencastproject.util.data.functions;

import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.nil;

import org.opencastproject.util.data.Function;

import java.text.Format;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Functions for strings. */
public final class Strings {

  private Strings() {
  }

  private static final List<String> NIL = nil();
  private static final Optional<String> NONE = Optional.empty();

  /**
   * Trim a string and return either <code>some</code> or <code>none</code> if it's empty. The string may be null.
   */
  public static final Function<String, Optional<String>> trimToNone = new Function<String, Optional<String>>() {
    @Override
    public Optional<String> apply(String a) {
      return trimToNone(a);
    }
  };

  /**
   * Trim a string and return either <code>some</code> or <code>none</code> if it's empty. The string may be null.
   */
  public static Optional<String> trimToNone(String a) {
    if (a != null) {
      final String trimmed = a.trim();
      return trimmed.length() > 0 ? Optional.of(trimmed) : NONE;
    } else {
      return Optional.empty();
    }
  }

  /** Return <code>a.toString()</code> wrapped in a some if <code>a != null</code>, none otherwise. */
  public static Optional<String> asString(Object a) {
    return a != null ? Optional.of(a.toString()) : NONE;
  }

  /** Return <code>a.toString()</code> wrapped in a some if <code>a != null</code>, none otherwise. */
  public static <A> Function<A, Optional<String>> asString() {
    return new Function<A, Optional<String>>() {
      @Override
      public Optional<String> apply(A a) {
        return asString(a);
      }
    };
  }

  /** Return <code>a.toString()</code> or <code>&lt;null&gt;</code> if argument is null. */
  public static <A> Function<A, String> asStringNull() {
    return new Function<A, String>() {
      @Override
      public String apply(A a) {
        return a != null ? a.toString() : "<null>";
      }
    };
  }

  /** Convert a string into a long if possible. */
  public static Optional<Double> toDouble(String s) {
    try {
      return Optional.of(Double.parseDouble(s));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  /** Convert a string into an integer if possible. */
  public static Optional<Integer> toInt(String s) {
    try {
      return Optional.of(Integer.parseInt(s));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  public static <A> Function<A, String> format(final Format f) {
    return new Function<A, String>() {
      @Override
      public String apply(A a) {
        return f.format(a);
      }
    };
  }

  public static final Function<String, List<String>> trimToNil = new Function<String, List<String>>() {
    @Override
    public List<String> apply(String a) {
      if (a != null) {
        final String trimmed = a.trim();
        return trimmed.length() > 0 ? list(trimmed) : NIL;
      } else {
        return NIL;
      }
    }
  };

  /** Create a {@linkplain Pattern#split(CharSequence) split} function from a regex pattern. */
  public static Function<String, String[]> split(final Pattern splitter) {
    return new Function<String, String[]>() {
      @Override
      public String[] apply(String s) {
        return splitter.split(s);
      }
    };
  }

}
