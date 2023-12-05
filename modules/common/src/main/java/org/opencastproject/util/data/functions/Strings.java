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


package org.opencastproject.util.data.functions;

import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;

import java.text.Format;
import java.util.List;
import java.util.regex.Pattern;

/** Functions for strings. */
public final class Strings {

  private Strings() {
  }

  private static final List<String> NIL = nil();
  private static final Option<String> NONE = none();

  /**
   * Trim a string and return either <code>some</code> or <code>none</code> if it's empty. The string may be null.
   */
  public static final Function<String, Option<String>> trimToNone = new Function<String, Option<String>>() {
    @Override
    public Option<String> apply(String a) {
      return trimToNone(a);
    }
  };

  /**
   * Trim a string and return either <code>some</code> or <code>none</code> if it's empty. The string may be null.
   */
  public static Option<String> trimToNone(String a) {
    if (a != null) {
      final String trimmed = a.trim();
      return trimmed.length() > 0 ? some(trimmed) : NONE;
    } else {
      return none();
    }
  }

  /** Return <code>a.toString()</code> wrapped in a some if <code>a != null</code>, none otherwise. */
  public static Option<String> asString(Object a) {
    return a != null ? some(a.toString()) : NONE;
  }

  /** Return <code>a.toString()</code> wrapped in a some if <code>a != null</code>, none otherwise. */
  public static <A> Function<A, Option<String>> asString() {
    return new Function<A, Option<String>>() {
      @Override
      public Option<String> apply(A a) {
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
  public static final Function<String, Option<Double>> toDouble = new Function<String, Option<Double>>() {
    @Override
    public Option<Double> apply(String s) {
      try {
        return some(Double.parseDouble(s));
      } catch (NumberFormatException e) {
        return none();
      }
    }
  };

  /** Convert a string into an integer if possible. */
  public static final Function<String, Option<Integer>> toInt = new Function<String, Option<Integer>>() {
    @Override
    public Option<Integer> apply(String s) {
      try {
        return some(Integer.parseInt(s));
      } catch (NumberFormatException e) {
        return none();
      }
    }
  };

  /**
   * Convert a string into a boolean.
   *
   * @see Boolean#valueOf(String)
   */
  public static final Function<String, Boolean> toBool = new Function<String, Boolean>() {
    @Override
    public Boolean apply(String s) {
      return Boolean.valueOf(s);
    }
  };

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
