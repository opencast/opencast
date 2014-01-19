/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.util.data.functions;

import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.util.data.Arrays;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Predicate;

import org.apache.commons.lang.StringUtils;

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
  public static final Function<String, Option<Long>> toLong = new Function<String, Option<Long>>() {
    @Override
    public Option<Long> apply(String s) {
      try {
        return some(Long.parseLong(s));
      } catch (NumberFormatException e) {
        return none();
      }
    }
  };

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

  /** Convert a string into an integer if possible. */
  public static final Function<String, List<Integer>> toIntL = new Function<String, List<Integer>>() {
    @Override
    public List<Integer> apply(String s) {
      try {
        return list(Integer.parseInt(s));
      } catch (NumberFormatException e) {
        return nil();
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

  /**
   * Return a string formatting function.
   * 
   * @see String#format(String, Object...)
   */
  public static <A> Function2<String, A[], String> format() {
    return new Function2<String, A[], String>() {
      @Override
      public String apply(String s, A[] p) {
        return String.format(s, p);
      }
    };
  }

  public static <A> Function<A, String> format(final Format f) {
    return new Function<A, String>() {
      @Override
      public String apply(A a) {
        return f.format(a);
      }
    };
  }

  public static final Predicate<String> notBlank = new Predicate<String>() {
    @Override
    public Boolean apply(String a) {
      return StringUtils.isBlank(a);
    }
  };

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

  public static Predicate<String> eqIgnoreCase(final String other) {
    return new Predicate<String>() {
      @Override
      public Boolean apply(String s) {
        return s.equalsIgnoreCase(other);
      }
    };
  }

  /**
   * Return a function that replaces all occurrences of <code>regex</code> in the argument with <code>replacement</code>
   * .
   * 
   * @see String#replaceAll(String, String)
   */
  public static Function<String, String> replaceAll(final String regex, final String replacement) {
    return new Function<String, String>() {
      @Override
      public String apply(String s) {
        return s.replaceAll(regex, replacement);
      }
    };
  }

  /** Create a split function from a regex pattern. */
  public static Function<String, String[]> split(final Pattern splitter) {
    return new Function<String, String[]>() {
      @Override
      public String[] apply(String s) {
        return splitter.split(s);
      }
    };
  }

  /**
   * Split function to split comma separated values. Regex = <code>\s*,\s*</code>.
   */
  public static final Function<String, String[]> csvSplit = split(Pattern.compile("\\s*,\\s*"));

  /**
   * Split function to split comma separated values. Regex = <code>\s*,\s*</code>.
   */
  public static final Function<String, List<String>> csvSplitList = Arrays.<String> toList().o(csvSplit);

  /** A function to prepend the argument string with a prefix. */
  public static Function<String, String> prepend(final String prefix) {
    return new Function<String, String>() {
      @Override
      public String apply(String s) {
        return prefix + s;
      }
    };
  }

  /** A function to append a suffix to the argument string. */
  public static Function<String, String> append(final String suffix) {
    return new Function<String, String>() {
      @Override
      public String apply(String s) {
        return s + suffix;
      }
    };
  }

  public static final Function<String, String> lowerCase = new Function<String, String>() {
    @Override
    public String apply(String s) {
      return s.toLowerCase();
    }
  };

  public static Predicate<String> matches(final String pattern) {
    return new Predicate<String>() {
      @Override
      public Boolean apply(String s) {
        return s.matches(pattern);
      }
    };
  }

  public static Predicate<String> contains(final String seq) {
    return new Predicate<String>() {
      @Override
      public Boolean apply(String s) {
        return s.contains(seq);
      }
    };
  }

  public static Function2<String, String, String> concat(final String sep) {
    return new Function2<String, String, String>() {
      @Override
      public String apply(String a, String b) {
        return a + sep + b;
      }
    };
  }
}
