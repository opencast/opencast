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


package org.opencastproject.oaipmh.server;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static org.opencastproject.oaipmh.OaiPmhUtil.fromUtc;

/**
 * Collection of general purpose functions.
 */
public final class Functions {

  private static final Logger logger = LoggerFactory.getLogger(Functions.class);

  /**
   * Converts a UTC string into a date. May throw a {@link org.opencastproject.oaipmh.server.OaiPmhRepository.BadArgumentException}.
   */
  public static final Function<String, Date> asDate = new Function<String, Date>() {
    @Override
    public Date apply(String s) {
      try {
        return fromUtc(s);
      } catch (ParseException e) {
        throw new OaiPmhRepository.BadArgumentException();
      }
    }
  };

  private Functions() {
  }

  /**
   * Return a function that appends to the given list.
   */
  public static <A> Function<A, List<A>> appendTo(final List<A> list) {
    return new Function<A, List<A>>() {
      @Override
      public List<A> apply(A a) {
        list.add(a);
        return list;
      }
    };
  }

  /**
   * Return a function that checks if a date is after the given date <code>d</code>.
   */
  public static Function<Date, Boolean> isAfter(final Option<Date> d) {
    return new Function<Date, Boolean>() {
      @Override
      public Boolean apply(Date date) {
        return d.isSome() && date.after(d.get());
      }
    };
  }

  /**
   * Return a function that just provides a values and logs a warning message.
   */
  public static <A> Function0<A> defaultValue(final A dflt, final String valName) {
    return new Function0<A>() {
      @Override
      public A apply() {
        logger.warn(valName + " should not be null. Using default value >" + dflt.toString() + "<");
        return dflt;
      }
    };
  }

}
