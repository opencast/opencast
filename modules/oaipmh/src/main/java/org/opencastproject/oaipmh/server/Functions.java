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

import static org.opencastproject.oaipmh.OaiPmhUtil.fromUtc;

import org.opencastproject.util.data.Function;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Collection of general purpose functions.
 */
public final class Functions {

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

  public static Function<Date, Date> addDay(final int days) {
    return new Function<Date, Date>() {
      @Override public Date apply(Date date) {
        final Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        c.setTime(date);
        c.add(Calendar.DAY_OF_MONTH, days);
        return c.getTime();
      }
    };
  }

  private Functions() {
  }

}
