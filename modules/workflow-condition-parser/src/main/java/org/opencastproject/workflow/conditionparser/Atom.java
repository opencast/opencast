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
package org.opencastproject.workflow.conditionparser;

import java.util.Objects;

/**
 * Represents a "value" in the condition language, so a string or a number, with corresponding comparison functions
 */
public final class Atom implements Comparable<Atom> {
  private final Double number;
  private final String string;

  private Atom(Double number, String string) {
    this.number = number;
    this.string = string;
  }

  @Override
  public String toString() {
    if (number != null) {
      final String s = String.valueOf(number);
      // This is a simple little hack to support something like 3+'4' to be converted to '34' instead of '3.04'. It's
      // dirty, but you maybe shouldn't be doing 3+'4' anyways.
      if (s.endsWith(".0")) {
        return s.substring(0, s.length() - 2);
      }
      return s;
    }
    return string;
  }

  private static Atom fromNumber(double number) {
    return new Atom(number, null);
  }

  static Atom fromString(String string) {
    return new Atom(null, string);
  }

  static Atom parseNumber(String text) {
    return new Atom(Double.parseDouble(text), null);
  }

  static Atom parseString(String text) {
    return new Atom(null, text);
  }

  Atom reduce(Atom atom, NumericalOperator op) {
    switch (op) {
      case ADD:
        if (number != null && atom.number != null) {
          return Atom.fromNumber(number + atom.number);
        }
        return Atom.fromString(toString() + atom.toString());
      case SUBTRACT:
        if (number != null) {
          return Atom.fromNumber(number - atom.number);
        }
        throw new IllegalArgumentException("Tried to subtract from string '" + string + "'");
      case MULTIPLY:
        if (number != null) {
          return Atom.fromNumber(number * atom.number);
        }
        throw new IllegalArgumentException("Tried to multiply with string '" + string + "'");
      default:
        if (number != null) {
          return Atom.fromNumber(number / atom.number);
        }
        throw new IllegalArgumentException("Tried to divide from string '" + string + "'");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Atom atom = (Atom) o;
    return Objects.equals(number, atom.number) && Objects.equals(string, atom.string);
  }

  @Override
  public int hashCode() {
    return Objects.hash(number, string);
  }

  @Override
  public int compareTo(Atom o) {
    if (number != null) {
      if (o.number == null) {
        return toString().compareTo(o.string);
      }
      return number.compareTo(o.number);
    }
    if (o.string == null) {
      return string.compareTo(o.toString());
    }
    return string.compareTo(o.string);
  }
}
